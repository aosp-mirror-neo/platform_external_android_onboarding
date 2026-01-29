package com.android.onboarding.tasks

import android.os.PersistableBundle
import android.os.Process.myUserHandle
import androidx.annotation.VisibleForTesting
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.datastore.OnboardingDataStore
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.metadata.SensitiveString
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.generateNodeId
import com.android.onboarding.nodes.refersTo
import com.android.onboarding.pending.BackgroundLaunchableForResult
import com.android.onboarding.tasks.service.OnboardingTaskService
import com.android.onboarding.tasks.service.OnboardingTaskServiceException
import com.android.onboarding.tasks.service.OnboardingTaskServiceServer
import com.android.onboarding.utils.KnownStableEquality
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.TimeSource
import com.android.onboarding.utils.logging.OnboardingLog
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.getPersistableList
import com.android.onboarding.utils.persistable.putPersistable
import com.android.onboarding.utils.persistable.putPersistableList
import com.android.onboarding.versions.OnboardingChanges
import com.android.onboarding.versions.changes.CHANGE_ID_FULL_LOCAL_BINDER
import com.android.onboarding.versions.changes.STORE_TASK_STATES_CHANGE_ID
import com.google.common.util.concurrent.ListenableFuture
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base class for managing the execution and state of onboarding tasks within the onboarding
 * process. This class provides common part of implementation for triggering tasks, monitoring their
 * progress, and obtaining results from onboarding tasks.
 *
 * It is expected that all implementations of this base are implemented as singletons.
 *
 * This triages incoming requests depending on whether the requester and the server are on the same
 * process or not.
 * - Cross-process: The requests first go through the requester's [CrossProcessTaskManager] to bind
 *   to the server's [OnboardingTaskManagerServiceBinder] to do the real work.
 * - Same-process: The requests go through the requester's [OnboardingTaskManagerServiceBinder]
 *   directly to do the real work.
 *
 * @param coroutineScope Coroutine scope to be used for launching async tasks and creating
 *   [ListenableFuture].
 * @param localServer Binder for same-process operations. This is only needed if the app isn't
 *   capable of executing same-process tasks through Android service binding.
 * @property crossProcessTaskManager [CrossProcessTaskManager] to be used for cross-process
 *   execution. Defaults to [DefaultCrossProcessTaskManager]. Can be set to other instance for
 *   testing purposes.
 * @param taskDataStore [OnboardingDataStore] to persist task data.
 */
@InternalOnboardingApi
internal class DefaultOnboardingTaskManager(
  private val component: ComponentRef,
  coroutineContext: CoroutineContext,
  private val localServer: OnboardingTaskServiceServer?,
  private val onboardingChanges: OnboardingChanges,
  private val taskDataStore: OnboardingDataStore,
  private val crossProcessTaskManager: CrossProcessTaskManager,
  private val timeSource: TimeSource,
) : OnboardingTaskManager, ComponentRef by component.asComponentRef() {
  override val id: NodeId = generateNodeId()
  override val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())


  /** A list of task execution history in the ascending order of execution time. */
  private val taskExecutionHistory = ConcurrentLinkedQueue<TaskExecutionHistory>()

  // We only need to restore the task execution history once per object lifecycle.
  private var restoreDeferredLazy = scheduleRestoreExecutionHistory()

  /** Generates a new [OnboardingTaskToken] for given [contract] execution with [input]. */
  override fun <I, O> generateToken(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O> =
    OnboardingTaskToken(
      taskManagerId = id,
      contract = contract,
      nodeId = generateNodeId(),
      targetUserId = contract.getTargetUserId(input),
      targetPackageName = contract.getTargetComponent(input),
    )

  override fun close() {
    super.close()
    coroutineScope.cancel()
  }

  override fun clearCachedTaskStates() {
    log.w("clearCachedTaskStates")
    taskExecutionHistory.clear()
    restoreDeferredLazy = scheduleRestoreExecutionHistory()
  }

  override suspend fun <I, O> runTask(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O> =
    withContext(coroutineScope.coroutineContext) {
      val token = generateToken(contract, input)

      val contractInputHash = contract.generateHash(input)
      restorePersistedTaskExecutionIfNecessary()
      val history =
        TaskExecutionHistory(
          contractInputHash = contractInputHash,
          token = token.toErased(),
          executionTime = timeSource.instant(),
        )
      taskExecutionHistory += history
      val unused = coroutineScope.launch { persistTaskExecutionHistory() }
      log.d("Cached $contractInputHash -> $token")

      withService(
        token,
        onError = {
          log.w("Failed to start task for $token", it)
          Unit
        },
      ) {
        startTask(token, contract.inputEncoder.encode(input))
      }
      return@withContext token
    }

  override fun <I, O> runTaskSynchronous(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O>? {
    if (!isTaskRunInSameComponent(contract)) {
      log.d(
        "Attempted to run task synchronously but $contract (${contract.nodePackage}) is not running in same process ($nodePackage) "
      )
      return null
    }

    if (localServer == null) {
      log.d("Attempted to run task state synchronously but localServer is null")
      return null
    }

    if (!localServer.isSynchronousTask(contract)) {
      log.d(
        "Attempted to run task state synchronously but $contract is not implemented synchronously"
      )
      return null
    }

    val token = generateToken(contract, input)
    try {
      if (!localServer.runTaskSynchronous(token.toErased(), contract.inputEncoder.encode(input))) {
        log.d("runTaskSynchronous for $token returned false")
        return null
      }
    } catch (e: Exception) {
      log.e("Error calling runTaskSynchronous", e)
      return null
    }

    return token
  }

  override fun <I, O> runTaskAsync(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): ListenableFuture<OnboardingTaskToken<O>> = coroutineScope.future { runTask(contract, input) }

  override suspend fun <I, O> runOrResumeTask(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O> =
    withContext(coroutineScope.coroutineContext) {
      val contractInputHash = contract.generateHash(input)
      log.d("contractInputHash: $contractInputHash")

      restorePersistedTaskExecutionIfNecessary()
      taskExecutionHistory
        .filter { it.contractInputHash == contractInputHash }
        .maxByOrNull { it.executionTime }
        ?.let {
          log.d("Returning existing token: ${it.token} for hash $contractInputHash")
          return@withContext it.token.resolve(contract)
        }

      return@withContext runTask(contract, input)
    }

  override fun <I, O> runOrResumeTaskSynchronous(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O>? {
    val contractInputHash = contract.generateHash(input)
    log.d("contractInputHash: $contractInputHash")

    val existingToken =
      taskExecutionHistory
        .filter { it.contractInputHash == contractInputHash }
        .maxByOrNull { it.executionTime }

    if (existingToken == null) {
      return runTaskSynchronous(contract, input)
    }

    val resolvedToken = existingToken.token.resolve(contract)
    if (waitForCompletedSynchronous(resolvedToken) == null) {
      return null
    }

    return resolvedToken
  }

  override fun <I, O> runOrResumeTaskAsync(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): ListenableFuture<OnboardingTaskToken<O>> =
    coroutineScope.future { runOrResumeTask(contract, input) }

  override suspend fun <I, O> runTaskAndGetResult(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskState.Completed<O> =
    withContext(coroutineScope.coroutineContext) {
      val token = runTask(contract, input)
      return@withContext waitForCompleted(token)
    }

  override fun <I, O> runTaskAndGetResultAsync(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): ListenableFuture<OnboardingTaskState.Completed<O>> =
    coroutineScope.future { runTaskAndGetResult(contract, input) }

  override suspend fun <I, O> runOrResumeTaskAndGetResult(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskState.Completed<O> =
    withContext(coroutineScope.coroutineContext) {
      val token = runOrResumeTask(contract, input)
      return@withContext waitForCompleted(token)
    }

  override fun <I, O> runOrResumeTaskAndGetResultAsync(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): ListenableFuture<OnboardingTaskState.Completed<O>> =
    coroutineScope.future { runOrResumeTaskAndGetResult(contract, input) }

  override suspend fun <O> getTaskState(token: OnboardingTaskToken<O>): OnboardingTaskState<O> =
    withContext(coroutineScope.coroutineContext) {
      log.i("Get task state: $token")
      withService(
        token,
        onError = {
          log.w("Failed to fetch task state for $token - assuming failure", it)
          OnboardingTaskState.Failure(token, errorMessage = SensitiveString(it.message))
        },
      ) {
        getTaskState(token)
      }
    }

  override fun <O> getTaskStateSynchronous(token: OnboardingTaskToken<O>): OnboardingTaskState<O>? {
    if (!isTaskRunInSameComponent(token)) {
      log.e(
        "Attempted to get task state synchronously but $token (${token.nodePackage}) is not running in same process ($nodePackage) "
      )
      return null
    }
    if (localServer == null) {
      log.e("Attempted to get task state synchronously but localServer is null")
      return null
    }
    return localServer.getTaskStateDirectly(token)
  }

  override fun <O> getTaskStateAsync(
    token: OnboardingTaskToken<O>
  ): ListenableFuture<OnboardingTaskState<O>> = coroutineScope.future { getTaskState(token) }

  override suspend fun <O> waitForCompleted(
    token: OnboardingTaskToken<O>
  ): OnboardingTaskState.Completed<O> {
    log.i("waitForCompleted: $token")
    return withService(
      token,
      onError = {
        log.w("Failed to await task completion for $token - returning implicit failure state", it)

        OnboardingTaskState.Failure(token, SensitiveString(it.message))
      },
    ) {
      awaitTaskCompletion(token)
    }
  }

  override fun <O> waitForCompletedSynchronous(
    token: OnboardingTaskToken<O>
  ): OnboardingTaskState.Completed<O>? {
    val state = getTaskStateSynchronous(token)

    if (state is OnboardingTaskState.Completed<O>) {
      return state
    }

    return null
  }

  override fun <O> waitForCompletedAsync(
    token: OnboardingTaskToken<O>
  ): ListenableFuture<OnboardingTaskState.Completed<O>> =
    coroutineScope.future { waitForCompleted(token) }

  override fun <O> getTaskMetadataFlow(
    token: OnboardingTaskToken<O>
  ): Flow<OnboardingMetadataStore> {
    log.i("getTaskMetadataFlow: $token")
    return flow {
      val metadata =
        withService(
          token,
          onError = {
            log.w("Error observing task metadata for $token", it)
            emptyFlow()
          },
        ) {
          observeTaskMetadata(token)
        }
      try {
        emitAll(metadata)
      } catch (e: OnboardingTaskServiceException) {
        log.w("Service exception while observing metadata for $token", e)
      }
    }
  }

  override fun <O> getTaskMetadataSynchronous(
    token: OnboardingTaskToken<O>
  ): OnboardingMetadataStore? {
    if (!isTaskRunInSameComponent(token)) {
      log.d(
        "Attempted to get task metadata synchronously but $token (${token.nodePackage}) is not running in same process ($nodePackage) "
      )
      return null
    }
    if (localServer == null) {
      log.e("Attempted to get task metadata synchronously but localServer is null")
      return null
    }
    if (!localServer.isSynchronousTask(token)) {
      log.d(
        "Attempted to get task metadata synchronously but $token is not implemented synchronously"
      )
      return null
    }
    return localServer.getTaskMetadataDirectly(token)
  }

  override suspend fun interrupt(token: OnboardingTaskToken<*>, reason: InterruptReason) {
    log.i("interrupt: $token with $reason")
    withService(
      token,
      onError = {
        log.w("Error interrupting task for $token", it)
        Unit
      },
    ) {
      interruptTask(token, reason)
    }
  }

  private fun isTaskRunInSameComponent(nodeRef: NodeRef): Boolean {
    return nodeRef refersTo this &&
      (onboardingChanges.componentSupportsChange(nodeComponent, CHANGE_ID_FULL_LOCAL_BINDER) ||
        // Implicitly include local path for components that used local binder before b/441696383.
        nodeRef.nodeComponent in
          setOf(
            "com.google.android.apps.setupwizard.searchselector/searchselector",
            "com.google.android.setupwizard/setupwizard",
            "com.google.android.gms/auth",
          ))
  }

  private suspend fun persistTaskExecutionHistory() {
    if (!onboardingChanges.currentProcessSupportsChange(STORE_TASK_STATES_CHANGE_ID)) {
      return
    }
    restorePersistedTaskExecutionIfNecessary()
    taskDataStore.write(
      KEY_TASK_EXECUTION_HISTORY_BUNDLE,
      PersistableTaskExecutionHistories(taskExecutionHistory.toList()),
    )
  }

  private fun scheduleRestoreExecutionHistory(): Lazy<Deferred<Unit>> {
    return lazy {
      coroutineScope.async {
        if (!onboardingChanges.currentProcessSupportsChange(STORE_TASK_STATES_CHANGE_ID)) {
          return@async
        }
        taskExecutionHistory.clear()
        taskDataStore
          .read(KEY_TASK_EXECUTION_HISTORY_BUNDLE, PersistableTaskExecutionHistories)
          ?.histories
          ?.let(taskExecutionHistory::addAll)
        log.i("Restored task execution history: $taskExecutionHistory")
        return@async
      }
    }
  }

  private suspend fun restorePersistedTaskExecutionIfNecessary() {
    if (!onboardingChanges.currentProcessSupportsChange(STORE_TASK_STATES_CHANGE_ID)) {
      return
    }
    restoreDeferredLazy.value.await()
  }

  /**
   * A task execution history.
   *
   * @param contractInputHash the hash for the combination of the contract and the input. It can be
   *   used to locate an execution entry.
   * @param token the unique identifier of a task execution.
   * @param executionTime the execution time of the task.
   */
  @VisibleForTesting
  data class TaskExecutionHistory(
    val contractInputHash: Int,
    val token: ErasedOnboardingTaskToken,
    val executionTime: @KnownStableEquality Instant,
  ) : Persistable, StableEquality {
    override fun toPersistableBundle(): PersistableBundle =
      PersistableBundle().apply {
        putInt(KEY_CONTRACT_INPUT_HASH, contractInputHash)
        putPersistable(KEY_TOKEN, token)
        putLong(KEY_EXECUTION_TIME, executionTime.toEpochMilli())
      }

    companion object : PersistableDeserializer<TaskExecutionHistory> {
      private const val KEY_CONTRACT_INPUT_HASH = "contractInputHash"
      private const val KEY_TOKEN = "token"
      private const val KEY_EXECUTION_TIME = "executionTime"

      override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
        TaskExecutionHistory(
          persistableBundle.getInt(KEY_CONTRACT_INPUT_HASH),
          requireNotNull(persistableBundle.getPersistable(KEY_TOKEN, ErasedOnboardingTaskToken)),
          Instant.ofEpochMilli(persistableBundle.getLong(KEY_EXECUTION_TIME)),
        )
    }
  }

  /** A [Persistable] representation of a list of [TaskExecutionHistory]. */
  private data class PersistableTaskExecutionHistories(val histories: List<TaskExecutionHistory>) :
    Persistable {
    override fun toPersistableBundle(): PersistableBundle =
      PersistableBundle().apply { putPersistableList(KEY_TASK_EXECUTION_HISTORY_LIST, histories) }

    companion object : PersistableDeserializer<PersistableTaskExecutionHistories> {
      private const val KEY_TASK_EXECUTION_HISTORY_LIST: String = "TaskExecutionHistoryList"

      override fun fromPersistableBundle(
        persistableBundle: PersistableBundle
      ): PersistableTaskExecutionHistories =
        PersistableTaskExecutionHistories(
          persistableBundle
            .getPersistableList(KEY_TASK_EXECUTION_HISTORY_LIST, TaskExecutionHistory)
            ?.filterNotNull() ?: error("Missing persistable task execution histories")
        )
    }
  }

  /**
   * Connect to [OnboardingTaskService] as needed and execute [action].
   *
   * [OnboardingTaskServiceException] errors are handled via [onError].
   *
   * All other errors are rethrown.
   */
  private suspend fun <O> withService(
    token: OnboardingTaskToken<*>,
    onError: (OnboardingTaskServiceException) -> O,
    action: suspend OnboardingTaskService.() -> O,
  ): O {
    return try {
      if (
        isTaskRunInSameComponent(token) &&
          token.targetUserId == myUserHandle().identifier &&
          localServer != null
      ) {
        log.v("Going through local server for same-process operation.")
        localServer.action()
      } else {
        crossProcessTaskManager.action()
      }
    } catch (e: OnboardingTaskServiceException) {
      log.w("Service returned an exception", e)
      onError(e)
    }
  }

  private companion object {
    val log = OnboardingLog.logger("DefaultOnboardingTaskManager")

    /**
     * The key for the [taskDataStore]. It holds historic entries of task executions history of
     * task-contract-input-hash to a list of [ErasedOnboardingTaskToken].
     *
     * @see taskExecutionHistory
     */
    const val KEY_TASK_EXECUTION_HISTORY_BUNDLE: String = "TaskExecutionHistoryBundle"
  }
}
