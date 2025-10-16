package com.android.onboarding.tasks

import android.content.Context
import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.datastore.OnboardingDataStore
import com.android.onboarding.encoding.Encoder
import com.android.onboarding.encoding.ResultHandler
import com.android.onboarding.metadata.NonSensitiveString
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.OnboardingEvent
import com.android.onboarding.nodes.OnboardingGraphLog
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.nodes.UniqueNodeRef
import com.android.onboarding.nodes.generateNodeId
import com.android.onboarding.process.KeepAliveManager
import com.android.onboarding.process.KeepAliveReason
import com.android.onboarding.tasks.security.TaskPermissionManager
import com.android.onboarding.tasks.service.IObserver
import com.android.onboarding.tasks.service.IObserverHandle
import com.android.onboarding.tasks.service.IPersistableBundleStream
import com.android.onboarding.tasks.service.IResultCallback
import com.android.onboarding.tasks.service.IUnitCallback
import com.android.onboarding.tasks.service.OnboardingTaskService
import com.android.onboarding.tasks.service.OnboardingTaskServiceServer
import com.android.onboarding.tasks.service.OnboardingTaskServiceServerException
import com.android.onboarding.tasks.service.PersistableBundleStream
import com.android.onboarding.tasks.service.asPersistable
import com.android.onboarding.tasks.service.await
import com.android.onboarding.tasks.utils.TaskResult
import com.android.onboarding.utils.datasplitter.BundleSplitter.chunkDataMap
import com.android.onboarding.utils.datasplitter.BundleSplitter.getFullLargeDataDeferred
import com.android.onboarding.utils.datasplitter.BundleSplitter.removeCachedData
import com.android.onboarding.utils.datasplitter.getPendingDataIdentifier
import com.android.onboarding.utils.datasplitter.isPendingBundle
import com.android.onboarding.utils.datasplitter.isTooLargeForTransaction
import com.android.onboarding.utils.datasplitter.persistableBundleOfChunks
import com.android.onboarding.utils.datasplitter.splitForTransaction
import com.android.onboarding.utils.datasplitter.toPendingBundle
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.sizeInBytes
import com.android.onboarding.versions.OnboardingChanges
import com.android.onboarding.versions.changes.CHANGE_ID_ASYNC_TASK_SERVER
import com.android.onboarding.versions.changes.SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID
import com.google.common.time.TimeSource
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope

/**
 * Default implementation of [IOnboardingTaskManagerService], deliberately not exposed to clients
 * directly.
 *
 * Clients should use [OnboardingTaskManagerServiceBinder] instead.
 */
@OptIn(InternalOnboardingApi::class)
abstract class DefaultOnboardingTaskManagerServiceBinder(
  component: ComponentRef,
  private val contextProvider: Supplier<Context>,
  private val tasks: Collection<Supplier<out OnboardingTask<*, *, *>>>,
  private val taskPermissionManager: TaskPermissionManager,
  private val keepAliveManager: KeepAliveManager,
  coroutineContext: CoroutineContext,
  private val onboardingChangesSupplier: Supplier<OnboardingChanges>,
  private val providedActivities: Set<OnboardingActivityApiContract<*, *>>,
  private val providedTasks: Set<OnboardingTaskContract<*, *>>,
  internal val taskStateManager: OnboardingTaskStateManager,
  crossProcessTaskManager: CrossProcessTaskManager,
  timeSource: TimeSource,
  taskDataStore: OnboardingDataStore,
  otm: ReadWriteProperty<Any?, OnboardingTaskManager>,
  val server: OnboardingTaskServiceServer,
) :
  IOnboardingTaskManagerService.Stub(),
  OnboardingTaskService by server,
  AutoCloseable,
  ComponentRef by component.asComponentRef() {

  init {
    var localOtm by otm
    localOtm =
      DefaultOnboardingTaskManager(
        component = asComponentRef(),
        coroutineContext = coroutineContext,
        localServer = server,
        onboardingChanges = onboardingChanges,
        taskDataStore = taskDataStore,
        crossProcessTaskManager = crossProcessTaskManager,
        timeSource = timeSource,
      )
    checkNotNull(localOtm)
  }

  val otm: OnboardingTaskManager by otm
  internal val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())

  internal inline val onboardingChanges: OnboardingChanges
    get() = onboardingChangesSupplier.get()

  internal inline val applicationContext: Context
    get() = contextProvider.get().applicationContext

  /**
   * A read-only registry of all the task definitions this manager understands. The registry is
   * indexed by the [OnboardingTaskContract] associated with each task.
   *
   * Additionally, relevant [ProviderTasks][ProviderTask] are injected as well.
   */
  private val taskRegistry: Map<NodeRef.Stable, Supplier<out OnboardingTask<*, *, *>>> by lazy {
    val providers =
      ProviderManager(
          component = asComponentRef(),
          tasks = providedTasks,
          activities = providedActivities,
        )
        .associate { it.asNodeRef() to Supplier { it } }
    ConcurrentHashMap(tasks.associateBy { it.get().contract.asNodeRef() } + providers)
  }

  /**
   * Executes given [block] in a [supervisorScope] and silently logs all potential errors to ensure
   * root [coroutineScope] does not get cancelled.
   */
  @CanIgnoreReturnValue
  private fun supervised(tag: String, block: suspend CoroutineScope.() -> Unit) =
    coroutineScope.launch {
      try {
        supervisorScope(block)
      } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Log.w(TAG, "Supervised execution has failed: $tag", e)
      }
    }

  /**
   * All contracts supported locally by this binder.
   *
   * Note that while this binder can be used to interact with tasks from any component, contracts
   * owned by components other than the current one will not be included in this set.
   */
  @VisibleForTesting
  val supportedLocalContractClasses: Set<KClass<out OnboardingTaskContract<*, *>>>
    get() =
      taskRegistry.values
        .map { it.get().contract }
        .filterIsInstance<OnboardingTaskContract<*, *>>()
        .map { it::class }
        .toSet()

  final override fun version(): Int =

    if (onboardingChanges.currentProcessSupportsChange(CHANGE_ID_ASYNC_TASK_SERVER)) {
      1
    } else {
      0
    }

  override fun startTaskAsync(
    token: IPersistableBundleStream,
    input: IPersistableBundleStream,
    callback: IUnitCallback,
  ) {
    supervised("startTaskAsync") {
      var eToken: ErasedOnboardingTaskToken? = null
      try {
        eToken = token.asPersistable(ErasedOnboardingTaskToken)
        startTaskAsync(eToken, input.await())
        callback.onSuccess()
      } catch (e: Exception) {
        callback.onError(serialiseException(eToken, e))
        if (e is CancellationException) throw e
      }
    }
  }

  suspend fun startTaskAsync(token: ErasedOnboardingTaskToken, input: PersistableBundle) =
    server.startTask(token.resolveWithLimitedAccess(), input)

  override fun getTaskStateAsync(token: IPersistableBundleStream, callback: IResultCallback) {
    supervised("getTaskStateAsync") {
      var eToken: ErasedOnboardingTaskToken? = null
      try {
        eToken = token.asPersistable(ErasedOnboardingTaskToken)
        val state = getTaskStateAsync(eToken)
        callback.onSuccess(PersistableBundleStream(state))
      } catch (e: Exception) {
        callback.onError(serialiseException(eToken, e))
        if (e is CancellationException) throw e
      }
    }
  }

  suspend fun getTaskStateAsync(token: ErasedOnboardingTaskToken): OnboardingTaskState<*> =
    server.getTaskState(token.resolveWithLimitedAccess())

  override fun observeTaskState(
    token: IPersistableBundleStream,
    observer: IObserver,
  ): IObserverHandle {
    val job =
      supervised("observeTaskState") {
        var eToken: ErasedOnboardingTaskToken? = null
        try {
          eToken = token.asPersistable(ErasedOnboardingTaskToken)
          observeTaskState(eToken).map(::PersistableBundleStream).collect(observer::onChange)
        } catch (e: Exception) {
          observer.onError(serialiseException(eToken, e))
          if (e is CancellationException) throw e
        } finally {
          observer.onClose()
        }
      }

    return IObserverHandle { job.cancel("State observer closed.") }
  }

  fun observeTaskState(token: ErasedOnboardingTaskToken): Flow<OnboardingTaskState<*>> =
    server.observeTaskState(token.resolveWithLimitedAccess())

  override fun getTaskMetadataAsync(token: IPersistableBundleStream, callback: IResultCallback) {
    supervised("getTaskMetadataAsync") {
      var eToken: ErasedOnboardingTaskToken? = null
      try {
        eToken = token.asPersistable(ErasedOnboardingTaskToken)
        val metadata = getTaskMetadataAsync(eToken)
        callback.onSuccess(PersistableBundleStream(metadata))
      } catch (e: Exception) {
        callback.onError(serialiseException(eToken, e))
        if (e is CancellationException) throw e
      }
    }
  }

  suspend fun getTaskMetadataAsync(token: ErasedOnboardingTaskToken) =
    server.getTaskMetadata(token.resolveWithLimitedAccess())

  override fun observeTaskMetadata(
    token: IPersistableBundleStream,
    observer: IObserver,
  ): IObserverHandle {
    val job =
      supervised("observeTaskMetadata") {
        var eToken: ErasedOnboardingTaskToken? = null
        try {
          eToken = token.asPersistable(ErasedOnboardingTaskToken)
          observeTaskMetadata(eToken).map(::PersistableBundleStream).collect(observer::onChange)
        } catch (e: Exception) {
          observer.onError(serialiseException(eToken, e))
          if (e is CancellationException) throw e
        } finally {
          observer.onClose()
        }
      }
    return IObserverHandle { job.cancel("Metadata observer closed.") }
  }

  fun observeTaskMetadata(token: ErasedOnboardingTaskToken): Flow<OnboardingMetadataStore> =
    server.observeTaskMetadata(token.resolveWithLimitedAccess())

  override fun interruptTaskAsync(
    token: IPersistableBundleStream,
    reason: IPersistableBundleStream,
    callback: IUnitCallback,
  ) {
    supervised("interruptTaskAsync") {
      var eToken: ErasedOnboardingTaskToken? = null
      try {
        eToken = token.asPersistable(ErasedOnboardingTaskToken)
        interruptTaskAsync(eToken, reason.asPersistable(InterruptReason))
        callback.onSuccess()
      } catch (e: Exception) {
        callback.onError(serialiseException(eToken, e))
        if (e is CancellationException) throw e
      }
    }
  }

  suspend fun interruptTaskAsync(token: ErasedOnboardingTaskToken, reason: InterruptReason) =
    server.interruptTask(token.resolveWithLimitedAccess(), reason)

  override fun awaitTaskCompletionAsync(
    token: IPersistableBundleStream,
    callback: IResultCallback,
  ) {
    supervised("awaitTaskCompletionAsync") {
      var eToken: ErasedOnboardingTaskToken? = null
      try {
        eToken = token.asPersistable(ErasedOnboardingTaskToken)
        val state = server.awaitTaskCompletion(eToken.resolveWithLimitedAccess())
        callback.onSuccess(PersistableBundleStream(state))
      } catch (e: Exception) {
        callback.onError(serialiseException(eToken, e))
        if (e is CancellationException) throw e
      }
    }
  }

  // LEGACY APIs

  @Deprecated("Use startTaskAsync", ReplaceWith("startTaskAsync"))
  @CanIgnoreReturnValue
  override fun startTask(token: ErasedOnboardingTaskToken, bundle: PersistableBundle): Boolean {
    Log.d(TAG, "startTask[$token]")

    if (onboardingChanges.currentProcessSupportsChange(CHANGE_ID_ASYNC_TASK_SERVER)) {
      return runBlocking(coroutineScope.coroutineContext) {
        try {
          startTaskAsync(token, getFullBundle(bundle))
          true
        } catch (e: Exception) {
          Log.w(TAG, "Task $token failed to start", e)
          if (e is CancellationException) throw e
          false
        }
      }
    }
    taskPermissionManager.enforceCallerPermission(applicationContext)
    val fullBundle = getFullBundle(bundle)
    return try {
      val task = createAndInitTask(token)
      if (task == null) {
        logCompleteTask(token, taskStateManager[token])
        return false
      }
      startTaskUnsafe(task, token, fullBundle)
      true
    } catch (e: Exception) {
      Log.w(TAG, IllegalStateException(e))
      false
    }
  }

  @Deprecated("Use getTaskStateWithCallback which supports restoring task states from disk.")
  override fun getTaskState(token: ErasedOnboardingTaskToken): ErasedOnboardingTaskState {
    Log.d(TAG, "getTaskState token=$token")

    if (onboardingChanges.currentProcessSupportsChange(CHANGE_ID_ASYNC_TASK_SERVER)) {
      val state =
        runBlocking(coroutineScope.coroutineContext) { getTaskStateAsync(token).toErased() }
      return getTaskStateWithLargeDataHandling(state)
    }
    taskPermissionManager.enforceCallerPermission(applicationContext)
    val state =
      try {
          getTaskStateUnsafe(token)
        } catch (e: Exception) {
          Log.w(TAG, IllegalStateException(e))
          OnboardingTaskState.Failure(token, NonSensitiveString(ERROR_RUNNING_TASK))
        }
        .toErased()
    return getTaskStateWithLargeDataHandling(state)
  }

  @Deprecated("Use getTaskStateAsync", ReplaceWith("getTaskStateAsync"))
  override fun getTaskStateWithCallback(
    token: ErasedOnboardingTaskToken,
    listener: IGetTaskStateListener,
  ) {

    supervised("getTaskStateWithCallback") {
      try {
        val state =
          if (onboardingChanges.currentProcessSupportsChange(CHANGE_ID_ASYNC_TASK_SERVER)) {
            getTaskStateAsync(token)
          } else {
            Log.d(TAG, "getTaskStateWithCallback token=$token")
            taskPermissionManager.enforceCallerPermission(applicationContext)
            taskStateManager.restoreTaskStatesIfNecessary()
            getTaskStateUnsafe(token)
          }
        listener.onTaskStateObtained(getTaskStateWithLargeDataHandling(state.toErased()))
      } catch (e: Exception) {
        Log.w(TAG, "Failed to fetch task state for $token", e)
        val state = OnboardingTaskState.Failure(token, NonSensitiveString(ERROR_RUNNING_TASK))
        listener.onTaskStateObtained(getTaskStateWithLargeDataHandling(state.toErased()))
        if (e is CancellationException) throw e
      }
    }
  }

  // This API needs to be kept around to respond to old clients that are still relying on this API
  // to poll for metadata changes. See b/419393232.
  @Deprecated("Use getTaskMetadataAsync", ReplaceWith("getTaskMetadataAsync"))
  override fun getTaskMetadata(token: ErasedOnboardingTaskToken): PersistableBundle {

    if (onboardingChanges.currentProcessSupportsChange(CHANGE_ID_ASYNC_TASK_SERVER)) {
      return runBlocking(coroutineScope.coroutineContext) {
        try {
          val metadata = getTaskMetadataAsync(token).toPersistableBundle()
          getPersistableBundleWithLargeDataHandling(metadata)
        } catch (e: Exception) {
          Log.w(TAG, "Failed to fetch task metadata for $token", e)
          if (e is CancellationException) throw e
          PersistableBundle()
        }
      }
    }
    taskPermissionManager.enforceCallerPermission(applicationContext)
    val metadata =
      try {
        getTaskMetadataUnsafe(token).toPersistableBundle()
      } catch (e: Exception) {
        Log.w(TAG, IllegalStateException(e))
        PersistableBundle()
      }
    return getPersistableBundleWithLargeDataHandling(metadata)
  }

  private fun getTaskMetadataFromCompletedState(
    state: UnresolvedOnboardingTaskState<*>
  ): PersistableBundle {
    val lastMetadata = state.lastMetadata
    return if (lastMetadata == null) {
      OnboardingMetadataStore().toPersistableBundle()
    } else {
      getPersistableBundleWithLargeDataHandling(lastMetadata.toPersistableBundle())
    }
  }

  @Deprecated("Use interruptTaskAsync", ReplaceWith("interruptTaskAsync"))
  override fun interrupt(token: ErasedOnboardingTaskToken, reason: PersistableBundle) {

    if (onboardingChanges.currentProcessSupportsChange(CHANGE_ID_ASYNC_TASK_SERVER)) {
      return runBlocking(coroutineScope.coroutineContext) {
        try {
          interruptTaskAsync(token, InterruptReason.fromPersistableBundle(reason))
        } catch (e: Exception) {
          Log.w(TAG, "Failed to interrupt task for $token", e)
          if (e is CancellationException) throw e
        }
      }
    }
    taskPermissionManager.enforceCallerPermission(applicationContext)
    val parsedReason = InterruptReason.fromPersistableBundle(reason)
    supervised("interrupt") {
      Log.w(TAG, "got interrupt for ${token.identity()} with reason $reason")
      val task = getTaskInstance(token)
      if (task == null) {
        Log.e(TAG, "No instance found for interrupt to token ${token.identity()}")
        return@supervised
      }
      if (!task.contract.canAcceptInterrupt(parsedReason)) {
        Log.d(TAG, "Task ${task.contract} does not accept interrupt $parsedReason")
        return@supervised
      }
      task.onInterrupt(parsedReason)
    }
  }

  @Deprecated("Use awaitTaskCompletionAsync", ReplaceWith("awaitTaskCompletionAsync"))
  override fun waitForCompleted(
    token: ErasedOnboardingTaskToken,
    listener: IOnboardingTaskManagerServiceListener,
  ) {

    if (onboardingChanges.currentProcessSupportsChange(CHANGE_ID_ASYNC_TASK_SERVER)) {
      supervised("waitForCompleted") {
        try {
          val state = server.awaitTaskCompletion(token.resolveWithLimitedAccess()).toErased()
          listener.onTaskCompleted(
            getTaskStateWithLargeDataHandling(state),
            getTaskMetadataFromCompletedState(state),
          )
        } catch (e: Exception) {
          Log.w(TAG, "Failed to await task completion for $token", e)
          val state =
            OnboardingTaskState.Failure(token, NonSensitiveString(ERROR_RUNNING_TASK)).toErased()
          listener.onTaskCompleted(
            getTaskStateWithLargeDataHandling(state),
            getTaskMetadataFromCompletedState(state),
          )
          if (e is CancellationException) throw e
        }
      }
      return
    }
    Log.d(TAG, "waitForCompleted token=$token")
    taskPermissionManager.enforceCallerPermission(applicationContext)

    supervised("waitForCompleted") {
      taskStateManager.restoreTaskStatesIfNecessary()
      var state = getTaskStateUnsafe(token)
      // We need to check against the ErasedOnboardingState in case the state is unresolved. This is
      // the case when the state was persisted in disk. The contract can't be persisted, hence can't
      // be resolved.
      if (state.isCompleted) {
        // Task is already complete.
        val state = state.toErased()
        Log.d(TAG, "[waitForCompleted] Flushing before calling onTaskCompleted.")
        taskStateManager.flush()
        listener.onTaskCompleted(
          getTaskStateWithLargeDataHandling(state),
          getTaskMetadataFromCompletedState(state),
        )
        return@supervised
      }

      // Waiting for task to complete.
      taskStateManager.getTaskExecution(token)?.join()
        ?: error("[waitForCompleted] Task is not started.")
      // Task is now complete.
      state = getTaskStateUnsafe(token).toErased()
      Log.d(TAG, "[waitForCompleted] Flushing before calling onTaskCompleted.")
      taskStateManager.flush()
      listener.onTaskCompleted(
        getTaskStateWithLargeDataHandling(state),
        getTaskMetadataFromCompletedState(state),
      )
    }
  }

  @Deprecated("Use observeTaskMetadata", ReplaceWith("observeTaskMetadata"))
  override fun listenForTaskMetadataChanges(
    token: ErasedOnboardingTaskToken,
    listener: IOnboardingTaskManagerServiceListener,
  ) {

    if (onboardingChanges.currentProcessSupportsChange(CHANGE_ID_ASYNC_TASK_SERVER)) {
      supervised("listenForTaskMetadataChanges") {
        try {
          // Ugly, but we need to do it to be consistent with legacy behaviour
          val currentState = server.getTaskState(token.resolveWithLimitedAccess()).toErased()
          if (currentState.isCompleted) {
            Log.d(TAG, "listenForTaskMetadataChanges#Flushing before calling onTaskCompleted.")
            taskStateManager.flush()
            listener.onTaskCompleted(
              /* state = */ getTaskStateWithLargeDataHandling(currentState),
              /* metadata = */ getTaskMetadataFromCompletedState(currentState),
            )
            return@supervised
          }
          observeTaskMetadata(token)
            .map(OnboardingMetadataStore::toPersistableBundle)
            .map(::getPersistableBundleWithLargeDataHandling)
            .collect(listener::onTaskMetadataUpdated)
          val finalState = server.awaitTaskCompletion(token.resolveWithLimitedAccess()).toErased()
          listener.onTaskCompleted(
            /* state = */ getTaskStateWithLargeDataHandling(finalState),
            /* metadata = */ getTaskMetadataFromCompletedState(finalState),
          )
        } catch (e: Exception) {
          Log.w(TAG, "Failed to observe task metadata for $token", e)
          if (e is CancellationException) throw e
        }
      }
      return
    }
    taskPermissionManager.enforceCallerPermission(applicationContext)

    supervised("listenForTaskMetadataChanges") {
      taskStateManager.restoreTaskStatesIfNecessary()

      if (getTaskStateUnsafe(token).isCompleted) {
        Log.d(TAG, "metadataChanges#Task($token) is already complete.")
        val state = getTaskStateUnsafe(token).toErased()
        Log.d(TAG, "listenForTaskMetadataChanges#Flushing before calling onTaskCompleted.")
        taskStateManager.flush()
        listener.onTaskCompleted(
          getTaskStateWithLargeDataHandling(state),
          getTaskMetadataFromCompletedState(state),
        )
        return@supervised
      }

      // Observe metadata channel until task completes.
      @Suppress("DEPRECATION") // Legacy code calling legacy APIs
      taskStateManager
        .getMetadataChannel(token)
        ?.consumeAsFlow()
        ?.distinctUntilChanged()
        ?.collect { metadata ->
          Log.d(TAG, "metadataChanges#Observed task($token) metadata update: $metadata")
          listener.onTaskMetadataUpdated(
            getPersistableBundleWithLargeDataHandling(metadata.toPersistableBundle())
          )
        }

      // The channel closes when the task completes.
      var state = getTaskStateUnsafe(token).toErased()
      // Ensure the task state is updated for extra safety. This is not usually needed.
      if (!state.isCompleted) {
        Log.w(TAG, "Task is not complete! Wait for it to complete. state=$state")
        taskStateManager.getTaskExecution(token)?.join()
        state = getTaskStateUnsafe(token).toErased()
        Log.d(TAG, "Task should now be complete. state=$state")
      }
      Log.d(TAG, "listenForTaskMetadataChanges#Flushing before calling onTaskCompleted.")
      taskStateManager.flush()
      listener.onTaskCompleted(
        getTaskStateWithLargeDataHandling(state),
        getTaskMetadataFromCompletedState(state),
      )
    }
  }

  /** This is the receiver side of the API. This receives the large data in chunks. */
  override fun sendDataChunk(
    token: ErasedOnboardingTaskToken,
    dataId: Int,
    chunk: ByteArray,
    chunkIndex: Int,
    totalChunks: Int,
    totalSizeInBytes: Int,
  ): Boolean {
    if (!onboardingChanges.currentProcessSupportsChange(SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID)) {
      Log.w(TAG, "Feature SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID is not enabled.")
      return false
    }
    taskPermissionManager.enforceCallerPermission(applicationContext)
    Log.d(
      TAG,
      "sendDataChunk#[$token] Receiving data chunk of id=$dataId chunk[${chunkIndex + 1}/$totalChunks]",
    )
    val chunks = chunkDataMap.getOrDefault(dataId, MutableList(totalChunks) { ByteArray(0) })
    chunks[chunkIndex] = chunk
    chunkDataMap[dataId] = chunks
    val receivedChunksSize = chunks.filter { it.isNotEmpty() }.size
    val receivedAllChunks = receivedChunksSize == totalChunks
    if (receivedAllChunks) {
      val fullData = persistableBundleOfChunks(chunks)
      Log.d(TAG, "Received full data of id=$dataId sizeInBytes=${fullData.sizeInBytes()}")
      getFullLargeDataDeferred(dataId).complete(fullData)
    }
    return receivedAllChunks
  }

  /** The is the receiver side of the API. This serves the data chunks to the fetcher. */
  override fun fetchDataChunk(
    token: ErasedOnboardingTaskToken,
    dataId: Int,
    chunkIndex: Int,
    totalChunks: Int,
    totalSizeInBytes: Int,
  ): ByteArray {
    if (!onboardingChanges.currentProcessSupportsChange(SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID)) {
      Log.w(TAG, "Feature SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID is not enabled.")
      return ByteArray(0)
    }
    taskPermissionManager.enforceCallerPermission(applicationContext)
    Log.d(TAG, "Fetching data chunk. id=$dataId [${chunkIndex + 1}/$totalChunks]")
    return chunkDataMap[dataId]?.get(chunkIndex) ?: error("can't fetch data chunk")
  }

  override fun close() {
    taskStateManager.close()
  }

  private fun logStartTask(
    task: OnboardingTask<*, *, *>,
    token: UnresolvedOnboardingTaskToken<*>,
    argument: PersistableBundle,
  ) {
    OnboardingGraphLog.log(
      OnboardingEvent.OnboardingTaskStartedEvent(
        sourceNodeId =
          -1,
        // (when .runTask is called)
        nodeName = task.nodeName,
        nodeComponent = task.nodeComponent,
        nodeId = task.nodeId,
        argument =
          try {
            task.contract.inputDecoder.encode(argument)
          } catch (e: Exception) {
            // this likely means the task is being started with some other input (e.g. chained
            // contracts)
            argument
          },
      )
    )
  }

  private fun logCompleteTask(
    node: UniqueNodeRef,
    state: OnboardingTaskState<*>,
    throwable: Throwable? = null,
  ) {
    val result =
      when (state) {
        is OnboardingTaskState.Success ->
          state.result as? TaskResult<*> ?: TaskResult.Success(state.result)

        is OnboardingTaskState.Failure ->
          state.result as? TaskResult<*>
            ?: TaskResult.Failure<Any>(throwable = throwable, failureReason = state.errorMessage)

        else -> {
          Log.wtf(TAG, "logCompleteTask called with non-complete task $node ($state)")
          null
        }
      }

    if (result != null) {
      OnboardingGraphLog.log(
        OnboardingEvent.OnboardingTaskCompleteEvent(
          nodeName = node.nodeName,
          nodeComponent = node.nodeComponent,
          nodeId = node.nodeId,
          result = result,
        )
      )
    }
  }

  /**
   * Creates and initializes a task given a [token] without running it.
   *
   * This is useful in tests where you want to process a task between creating it and running it.
   *
   * @see startTaskUnsafe
   */
  @VisibleForTesting
  fun createAndInitTask(token: UnresolvedOnboardingTaskToken<*>): OnboardingTask<*, *, *>? {
    val task = createTaskInstance(token)
    if (task == null) {
      Log.w(TAG, "Task for node $token not found!")
      // This could happen if service implementation did not register a task matching the
      // contract, due to an incorrect implementation of task contracts.
      taskStateManager[token] =
        OnboardingTaskState.Failure(
          nodeRef = token,
          nodeId = token.nodeId,
          errorMessage = NonSensitiveString(ERROR_TASK_CANNOT_BE_FOUND),
        )
    }
    return task
  }

  /**
   * Starts a task previously created by [createAndInitTask] with [token].
   *
   * This is useful in tests where you want to process a task between creating it and running it.
   *
   * @see createAndInitTask
   */
  @VisibleForTesting
  fun startTaskUnsafe(
    task: OnboardingTask<*, *, *>,
    token: UnresolvedOnboardingTaskToken<*>,
    input: PersistableBundle,
  ) {
    startLocalTask(task, token, input)
  }

  private fun startLocalTask(
    task: OnboardingTask<*, *, *>,
    token: UnresolvedOnboardingTaskToken<*>,
    input: PersistableBundle,
  ) {
    Log.i(TAG, "startLocalTask[$task]")
    if (onboardingChanges.currentProcessSupportsChange(SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID)) {
      check(!input.isPendingBundle()) { "Cannot run task with a pending bundle." }
    }

    val resolvedToken = token.resolve(task.contract)
    // Update the task state as in progress immediately before running the task.
    taskStateManager[token] = OnboardingTaskState.InProgress(resolvedToken)


    // Keep the service alive until the task (and its dependency tasks) is complete.
    val keepAliveToken = keepAliveManager.keepAlive(KeepAliveReason.OTHER)
    // Run the task asynchronously.
    executeTask(task, input, resolvedToken, keepAliveToken).let {
      // Cache the execution so that we can get back to it later.
      taskStateManager.cacheTaskExecution(token, it)
    }
  }

  /**
   * Execute the task asynchronously in the current process.
   *
   * @return a [Deferred] instance of this execution.
   */
  private fun executeTask(
    task: OnboardingTask<*, *, *>,
    input: PersistableBundle,
    token: OnboardingTaskToken<*>,
    keepAliveToken: AutoCloseable,
  ): Deferred<Unit> =
    coroutineScope.async {
      Log.d(TAG, "executeTask#Flushing InProgress state before executing the task.")
      taskStateManager.flush()

      logStartTask(task, token, input)
      Log.d(TAG, "executeTask#start[$token]")
      try {
        // Execute the task and await its completion.
        val taskState = task.execute(context = applicationContext, input = input)
        Log.d(TAG, "executeTask#end[$token] taskState=$taskState")

        // Update the tasksStates map with the actual task result after completion.
        taskStateManager[token] = taskState
        logCompleteTask(token, taskState)
      } catch (e: RuntimeException) {
        Log.w(TAG, "executeTask#Caught RuntimeException during task execution", e)
        logCompleteTask(token, taskStateManager[token], e)
      } finally {
        keepAliveToken.close()
        taskStateManager.closeMetadataChannel(token)
        task.onDestroy()
      }
    }

  /**
   * Creates a new task for [token], replacing any existing tasks which were running for the same
   * token.
   */
  private fun createTaskInstance(
    token: UnresolvedOnboardingTaskToken<*>
  ): OnboardingTask<*, *, out BaseOnboardingTaskContract<*, *>>? {
    val taskSupplier = taskRegistry[token.asNodeRef()] ?: return null

    val task = taskSupplier.get()
    Log.d(TAG, "createTaskInstance token=$token task=$task")
    if (task.isCreated) {
      Log.wtf(TAG, "Task ($task) was already created. Please don't reuse the same task instance.")
      // TaskSupplier doesn't always return a new task instance. It can return a singleton.

      // Returning [null] will result in a task not found error message for the latest execution.
      // Returning [task] will reuse the old task. The first execution will fail due to bad states.
      // The latter is worse because the error logs are more confusing.
      return null
    }
    initialiseTask(task, token)
    taskStateManager.initMetadataChannel(token)


    taskStateManager.cacheTaskInstance(token, task)
    return task
  }

  /**
   * Initialises the [task] with this binder context.
   *
   * @param task The [OnboardingTask] to initialise.
   * @param nodeRef The [UniqueNodeRef] to assign to the task. If not provided, will be extracted
   *   from the task or default to [generateNodeId].
   */
  fun initialiseTask(task: OnboardingTask<*, *, *>, nodeRef: UniqueNodeRef? = null) {
    val ref =
      nodeRef
        ?: runCatching(task::asUniqueNodeRef)
          .getOrDefault(UniqueNodeRef(node = task, nodeId = generateNodeId()))
    task.onCreate(
      OnboardingTask.OnboardingTaskContext(
        nodeRef = ref,
        taskStateManager = taskStateManager,
        coroutineContext = coroutineScope.coroutineContext,
        onboardingChanges = onboardingChanges,
        onboardingTaskManager = otm,
      )
    )
  }

  /** Returns a previous instantiated task instance for [token], or null if there isn't one. */
  private fun getTaskInstance(
    token: UnresolvedOnboardingTaskToken<*>
  ): OnboardingTask<*, *, out BaseOnboardingTaskContract<*, *>>? =
    taskStateManager.getTaskInstance(token)

  @VisibleForTesting
  fun getTaskStateUnsafe(
    token: UnresolvedOnboardingTaskToken<*>
  ): UnresolvedOnboardingTaskState<*> {
    Log.i(TAG, "getTaskStateUnsafe#[$token]")

    return taskStateManager[token]
  }

  private fun getTaskMetadataUnsafe(
    token: UnresolvedOnboardingTaskToken<*>
  ): OnboardingMetadataStore = taskStateManager.getMetadata(token)

  /**
   * Returns [bundle] if it's not a pending bundle. Otherwise it'll block and return the full data
   * when it's received.
   */
  private fun getFullBundle(bundle: PersistableBundle): PersistableBundle {
    if (!onboardingChanges.currentProcessSupportsChange(SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID)) {
      return bundle
    }
    if (!bundle.isPendingBundle()) {
      return bundle
    }
    // Block until full input bundle is received.
    Log.d(TAG, "Waiting until full bundle of id ${bundle.getPendingDataIdentifier()} is received.")
    val fullData =
      runBlocking(coroutineScope.coroutineContext) {
        getFullLargeDataDeferred(bundle.getPendingDataIdentifier()).await()
      }
    Log.d(TAG, "Received full bundle of id ${bundle.getPendingDataIdentifier()}")
    removeCachedData(bundle.getPendingDataIdentifier())
    return fullData
  }

  /**
   * If the [state] is too large for binder transaction, Returns a "pending state" representation of
   * it and cache the full data to be fetched later. Otherwise returns the [state] unchanged.
   */
  private fun getTaskStateWithLargeDataHandling(
    state: ErasedOnboardingTaskState
  ): ErasedOnboardingTaskState {
    if (!onboardingChanges.currentProcessSupportsChange(SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID)) {
      return state
    }
    if (!state.isTooLargeForTransaction()) {
      return state
    }
    // Prepare data in chunks to be fetched.
    val pendingState = state.toPendingState()
    val dataId = pendingState.getPendingDataIdentifier()
    val chunks = state.splitForTransaction()
    chunkDataMap[dataId] = chunks.toMutableList()
    Log.d(TAG, "Prepared data in chunks to be fetched. id=$dataId")
    return pendingState
  }

  /**
   * If the [persistableBundle] is too large for binder transaction, Returns a "pending bundle"
   * representation of it and cache the full data to be fetched later. Otherwise returns the
   * [persistableBundle] unchanged.
   */
  private fun getPersistableBundleWithLargeDataHandling(
    persistableBundle: PersistableBundle
  ): PersistableBundle {
    if (!onboardingChanges.currentProcessSupportsChange(SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID)) {
      return persistableBundle
    }
    if (!persistableBundle.isTooLargeForTransaction()) {
      return persistableBundle
    }
    // Prepare data in chunks to be fetched.
    val pendingBundle = persistableBundle.toPendingBundle()
    val dataId = pendingBundle.getPendingDataIdentifier()
    val chunks = persistableBundle.splitForTransaction()
    chunkDataMap[dataId] = chunks.toMutableList()
    Log.d(TAG, "Prepared data in chunks to be fetched. id=$dataId")
    return pendingBundle
  }

  /**
   * Resolves this [UnresolvedOnboardingTaskToken] to a special [OnboardingTaskToken] which lacks
   * access to [OnboardingTaskToken.taskServiceIntent] and [OnboardingTaskToken.resultHandler].
   *
   * Trying to access [OnboardingTaskToken.taskServiceIntent] or result will throw an
   * [IllegalStateException].
   */
  private fun UnresolvedOnboardingTaskToken<*>.resolveWithLimitedAccess():
    OnboardingTaskToken<Nothing> {
    return OnboardingTaskToken(
      taskManagerId = taskManagerId,
      nodeRef = asNodeRef(),
      taskServiceIntentProvider = Supplier { error("Resolved access not permitted!") },
      resultHandler =
        object : ResultHandler<PersistableBundle, Nothing> {
          override val resultDecoder: Encoder<PersistableBundle, Nothing> = Encoder {
            error("Resolved access not permitted!")
          }

          override val resultEncoder: Encoder<Nothing, PersistableBundle> = Encoder {
            error("Resolved access not permitted!")
          }
        },
      nodeId = nodeId,
      targetUserId = targetUserId,
      targetPackageName = targetPackageName,
    )
  }

  /**
   * Builds a [PersistableBundleStream] from this [Throwable] as
   * [OnboardingTaskServiceServerException].
   */
  private fun serialiseException(
    ref: UniqueNodeRef?,
    cause: Throwable,
    message: String = cause.message ?: "Unknown onboarding task service error.",
  ): PersistableBundleStream {
    if (cause is OnboardingTaskServiceServerException) return PersistableBundleStream(cause)
    return PersistableBundleStream(
      OnboardingTaskServiceServerException(
        ref = ref ?: UniqueNodeRef(NodeRef(this, "UNKNOWN"), UNKNOWN_NODE_ID),
        message = message,
        cause = cause,
      )
    )
  }

  private companion object {
    const val TAG = "OTMServiceBinder"
  }
}
