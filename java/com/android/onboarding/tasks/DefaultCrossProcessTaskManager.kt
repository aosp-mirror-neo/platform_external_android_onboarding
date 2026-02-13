package com.android.onboarding.tasks

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.DeadObjectException
import android.os.IBinder
import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.nodes.UniqueNodeRef
import com.android.onboarding.tasks.OnboardingTaskManagerServiceConnection.ConnectionListener
import com.android.onboarding.tasks.service.OnboardingTaskServiceClient
import com.android.onboarding.tasks.service.OnboardingTaskServiceClientException
import com.android.onboarding.tasks.service.OnboardingTaskServiceException
import com.android.onboarding.tasks.service.OnboardingTaskServiceServerException
import com.android.onboarding.utils.currentUserIdentifier
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.safeUnbindService
import com.android.onboarding.utils.userHandleOf
import com.android.onboarding.versions.OnboardingChanges
import com.android.onboarding.versions.changes.CHANGE_ID_ASYNC_TASK_CLIENT
import com.android.onboarding.versions.changes.CHANGE_ID_ASYNC_TASK_SERVER
import com.android.onboarding.versions.changes.CLOSE_SERVICE_CONNECTION_AFTER_RPC_ID
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

private const val TAG = "DefaultCrossProcessTaskManager"

/**
 * Default implementation of [CrossProcessTaskManager].
 *
 * @param contextProvider Application context for task execution service binding.
 */
@InternalOnboardingApi
class DefaultCrossProcessTaskManager(
  contextProvider: Supplier<Context>,
  private val onboardingChanges: OnboardingChanges,
) : CrossProcessTaskManager {
  private val applicationContext: Context by lazy { contextProvider.get().applicationContext }
  /**
   * A cached bound service registry mapping service [NodeRef] to
   * [OnboardingTaskManagerServiceConnection].
   */
  @VisibleForTesting
  val serviceRegistry = ConcurrentHashMap<NodeRef.Stable, OnboardingTaskManagerServiceConnection>()
  private val serviceUsage = ConcurrentHashMap<NodeRef.Stable, Int>()

  override suspend fun startTask(token: OnboardingTaskToken<*>, input: PersistableBundle) {
    val service = prepareService(token)
    Log.d(TAG, "startTask#token ${token.identity()} prepared service: $service")

    return service.execute(
      token,
      onError = { Log.w(TAG, "startTask#Task service failed for token ${token.identity()}", it) },
    ) {
      it.startTask(token, input)
    }
  }

  override suspend fun <O> getTaskState(token: OnboardingTaskToken<O>): OnboardingTaskState<O> {
    val service = prepareService(token)
    val state =
      service.execute(
        token,
        onError = { Log.w(TAG, "getTaskState#Task service died for token ${token.identity()}", it) },
      ) {
        it.getTaskState(token)
      }
    return state
  }

  override suspend fun getTaskMetadata(token: OnboardingTaskToken<*>): OnboardingMetadataStore {
    val service = prepareService(token)
    val metadata =
      service.execute(
        token,
        onError = {
          Log.w(TAG, "getTaskMetadata#Task service failed for token ${token.identity()}", it)
        },
      ) {
        it.getTaskMetadata(token)
      }
    return metadata
  }

  override suspend fun interruptTask(token: OnboardingTaskToken<*>, reason: InterruptReason) {
    val service = prepareService(token)
    service.execute(
      token,
      onError = {
        Log.w(
          TAG,
          "interruptTask#Task service returned an exception for token ${token.identity()}",
          it,
        )
      },
    ) {
      it.interruptTask(token, reason)
    }
  }

  @CanIgnoreReturnValue
  override suspend fun <O> awaitTaskCompletion(
    token: OnboardingTaskToken<O>
  ): OnboardingTaskState.Completed<O> {
    val service = prepareService(token)
    val state =
      service.execute(
        token,
        onError = {
          Log.w(TAG, "awaitTaskCompletion#Task service failed for token ${token.identity()}", it)
        },
      ) {
        it.awaitTaskCompletion(token)
      }
    return state
  }

  override fun <O> observeTaskState(token: OnboardingTaskToken<O>): Flow<OnboardingTaskState<O>> {
    val service = prepareService(token)
    return flow {
      val clientFlow =
        service.execute(
          token,
          onError = {
            Log.w(TAG, "observeTaskState#Task service died for token ${token.identity()}", it)
          },
        ) {
          it.observeTaskState(token)
        }
      try {
        clientFlow.collect {
          Log.d(TAG, "observeTaskState#onChange [$token] state=$it")
          emit(it)
        }
      } catch (e: DeadObjectException) {
        val clientException = OnboardingTaskServiceClientException(token, e, ERROR_SERVICE_DIED)
        Log.w(TAG, "Task service died while observing task state!", clientException)
        throw clientException
      }
    }
  }

  override fun observeTaskMetadata(token: OnboardingTaskToken<*>): Flow<OnboardingMetadataStore> {
    val service = prepareService(token)
    return flow {
      val clientFlow =
        service.execute(
          token,
          onError = {
            Log.w(TAG, "observeTaskMetadata#Task service died for token ${token.identity()}", it)
          },
        ) {
          it.observeTaskMetadata(token)
        }
      try {
        clientFlow.collect {
          Log.d(TAG, "observeTaskMetadata#onChange [$token] metadataStore=$it")
          emit(it)
        }
      } catch (e: DeadObjectException) {
        val clientException = OnboardingTaskServiceClientException(token, e, ERROR_SERVICE_DIED)
        Log.w(TAG, "Task service died while observing task metadata!", clientException)
        throw clientException
      }
    }
  }

  private fun prepareService(
    token: OnboardingTaskToken<*>
  ): OnboardingTaskManagerServiceConnection {
    val intent = token.taskServiceIntent
    val nodeRef = token.asNodeRef()
    synchronized(serviceRegistry) {
      val existingService = serviceRegistry[nodeRef]
      if (existingService != null) {
        synchronized(existingService) {
          if (!existingService.isClosed) {
            Log.d(TAG, "prepareService#Returning existing service $existingService for $nodeRef")
            increaseServiceUsage(nodeRef)
            return existingService
          } else {
            Log.d(TAG, "prepareService#Existing service is closed $existingService for $nodeRef")
            // The service is closed/closing, so we remove it (if it hasn't been already) and
            // create a new one.
            serviceRegistry.remove(nodeRef, existingService)
          }
        }
      }

      Log.d(TAG, "prepareService#Creating new service for $nodeRef")
      val listener =
        object : ConnectionListener {
          override fun onServiceConnected() {}

          override fun onServiceDisconnected() {
            serviceRegistry.remove(nodeRef)
            serviceUsage.remove(nodeRef)
          }

          override fun onConnectionClosed(connection: OnboardingTaskManagerServiceConnection) {
            serviceRegistry.remove(nodeRef, connection)
            serviceUsage.remove(nodeRef, 0)
          }
        }
      return OnboardingTaskManagerServiceConnection(
          context = applicationContext,
          intent = intent,
          targetUserId = token.targetUserId,
          listener = listener,
          onboardingChanges = onboardingChanges,
          targetComponent = token,
          serviceUsage = serviceUsage,
        )
        .also { ref ->
          Log.d(TAG, "Adding service $ref to serviceRegistry. ComponentRef=$nodeRef")
          serviceRegistry[nodeRef] = ref
          increaseServiceUsage(nodeRef)
        }
    }
  }

  private fun increaseServiceUsage(nodeRef: NodeRef.Stable) {
    synchronized(serviceUsage) {
      val newUsageCount = serviceUsage[nodeRef]?.inc() ?: 1
      serviceUsage[nodeRef] = newUsageCount
      Log.d(TAG, "Service connection usage for $nodeRef has increased to $newUsageCount")
    }
  }

  /**
   * Flushes all cached service bindings. It is safe to continue using this manager after calling
   * [close] as it will just open new service bindings on demand.
   */
  override fun close() {
    synchronized(serviceRegistry) {
      val services = serviceRegistry.values
      serviceRegistry.clear()
      serviceUsage.clear()
      for (service in services) {
        service.close()
      }
    }
  }
}

@InternalOnboardingApi
@VisibleForTesting
class OnboardingTaskManagerServiceConnection
private constructor(
  context: Context,
  private val listener: ConnectionListener,
  private val onboardingChanges: OnboardingChanges,
  private val targetComponent: ComponentRef,
  private val serviceUsage: ConcurrentHashMap<NodeRef.Stable, Int>,
) : AutoCloseable, ServiceConnection {
  private val context = WeakReference(context.applicationContext)
  private val service = MutableStateFlow<OnboardingTaskServiceClient?>(null)

  override fun onServiceConnected(name: ComponentName?, service: IBinder) {
    Log.i(TAG, "Service connected: $name instance=$this")

    val client =
      if (
        onboardingChanges.currentProcessSupportsChange(CHANGE_ID_ASYNC_TASK_CLIENT) &&
          onboardingChanges.componentSupportsChange(
            targetComponent.nodeComponent,
            CHANGE_ID_ASYNC_TASK_SERVER,
          )
      ) {
        OnboardingTaskServiceClient(targetComponent, onboardingChanges, service)
      } else {
        OnboardingTaskServiceClient.Compat(targetComponent, onboardingChanges, service)
      }
    this@OnboardingTaskManagerServiceConnection.service.compareAndSet(
      expect = null,
      update = client,
    )
    listener.onServiceConnected()
  }

  override fun onServiceDisconnected(name: ComponentName?) {
    Log.i(TAG, "Service disconnected: $name instance=$this")
    this@OnboardingTaskManagerServiceConnection.service.value = null
    listener.onServiceDisconnected()
  }

  /**
   * Suspend until the [IOnboardingTaskManagerService] is available and execute a given [action].
   *
   * Catches [DeadObjectException] and closes the service if it occurs.
   *
   * @param ref the target [UniqueNodeRef] an [action] is to be executed for.
   * @param onError a notify callback to be invoked on any expected errors. This callback does not
   *   suppress the error and will always rethrow it. It is meant for error observation to log it or
   *   reach to it in other ways.
   * @throws OnboardingTaskServiceException
   */
  suspend fun <R> execute(
    ref: UniqueNodeRef,
    onError: (OnboardingTaskServiceException) -> Unit,
    action: suspend (service: OnboardingTaskServiceClient) -> R,
  ): R =
    try {
      val server = service.filterNotNull().first()
      action(server)
    } catch (e: DeadObjectException) {
      Log.w(TAG, "Task service died!", e)
      closeRef(ref)
      val clientError = OnboardingTaskServiceClientException(ref, e, ERROR_SERVICE_DIED)
      onError(clientError)
      throw clientError
    } catch (e: OnboardingTaskServiceServerException) {
      onError(e)
      throw e
    } finally {
      if (
        onboardingChanges.currentProcessSupportsChange(CLOSE_SERVICE_CONNECTION_AFTER_RPC_ID) &&
          onboardingChanges.componentSupportsChange(
            targetComponent.nodeComponent,
            CLOSE_SERVICE_CONNECTION_AFTER_RPC_ID,
          )
      ) {
        closeRef(ref)
      }
    }

  /**
   * Decreases the service usage for [ref]. Close the corresponding service connection if there's no
   * longer any usage.
   */
  private fun closeRef(ref: UniqueNodeRef) {
    synchronized(this) {
      if (decreaseServiceUsage(ref.asNodeRef()) == 0) {
        close()
      }
    }
  }

  private fun decreaseServiceUsage(nodeRef: NodeRef.Stable): Int {
    synchronized(serviceUsage) {
      val newUsageCount = serviceUsage[nodeRef]?.dec() ?: 0
      serviceUsage[nodeRef] = newUsageCount
      Log.d(TAG, "Service connection usage for $nodeRef has decreased to $newUsageCount")
      return newUsageCount
    }
  }

  @VisibleForTesting
  var isClosed = false
    private set

  override fun close() {
    synchronized(this) {
      if (isClosed) return
      Log.i(TAG, "Closing service connection: $this")
      isClosed = true
    }
    // We remove the connection from the registry first to ensure no new clients get this dying
    // connection.
    listener.onConnectionClosed(this)
    this@OnboardingTaskManagerServiceConnection.service.value = null
    context.get()?.let(::safeUnbindService)
  }

  companion object {
    /**
     * Construct and bind the [OnboardingTaskManagerServiceConnection].
     *
     * @param intent prepared intent to use when binding the service.
     * @return bound connection if it was bound successfully or null.
     */
    operator fun invoke(
      context: Context,
      intent: Intent,
      targetUserId: Int,
      listener: ConnectionListener,
      onboardingChanges: OnboardingChanges,
      targetComponent: ComponentRef,
      serviceUsage: ConcurrentHashMap<NodeRef.Stable, Int>,
    ): OnboardingTaskManagerServiceConnection {
      val applicationContext = context.applicationContext
      val connection =
        OnboardingTaskManagerServiceConnection(
          applicationContext,
          listener,
          onboardingChanges,
          targetComponent.asComponentRef(),
          serviceUsage,
        )
      val isServiceBound =
        if (targetUserId != currentUserIdentifier) {
          bindServiceCrossUser(applicationContext, intent, connection, targetUserId)
        } else {
          bindService(applicationContext, intent, connection)
        }
      return if (!isServiceBound) {
        Log.e(TAG, "Failed to bind service: $intent")
        connection.safeUnbindService(applicationContext)
        throw OnboardingTaskServiceClientException(
          UniqueNodeRef(NodeRef(targetComponent, "UNKNOWN"), UNKNOWN_NODE_ID),
          IllegalStateException(ERROR_FAILED_BIND_TASK_SERVICE),
        )
      } else {
        Log.i(TAG, "Bound service: $intent")
        connection
      }
    }

    private fun bindService(
      context: Context,
      intent: Intent,
      connection: OnboardingTaskManagerServiceConnection,
    ): Boolean {
      try {
        Log.d(TAG, "Binding service: $intent")
        return context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to bind service: $intent", e)
        return false
      }
    }

    private fun bindServiceCrossUser(
      context: Context,
      intent: Intent,
      connection: OnboardingTaskManagerServiceConnection,
      targetUserId: Int,
    ): Boolean {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        Log.w(TAG, "Cross-user binding is not supported on pre-R devices.")
        return false
      }
      try {
        Log.i(
          TAG,
          "Binding cross-user: $intent from user $currentUserIdentifier to user $targetUserId",
        )
        return context.bindServiceAsUser(
          intent,
          connection,
          Context.BIND_AUTO_CREATE,
          userHandleOf(targetUserId),
        )
      } catch (e: Exception) {
        Log.w(
          TAG,
          "Failed to bind cross-user: $intent from user $currentUserIdentifier to user $targetUserId",
          e,
        )
        return false
      }
    }
  }

  /** A listener to listen for the status of the underlying [ServiceConnection]. */
  interface ConnectionListener {
    /**
     * The connection to a service is connected.
     *
     * @see onServiceConnected
     */
    fun onServiceConnected()

    /**
     * The connection to a service is disconnected.
     *
     * @see onServiceDisconnected
     */
    fun onServiceDisconnected()

    /**
     * The connection resource is closed.
     *
     * @see close
     */
    fun onConnectionClosed(connection: OnboardingTaskManagerServiceConnection)
  }
}
