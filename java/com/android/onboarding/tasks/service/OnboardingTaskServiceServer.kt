@file:OptIn(InternalOnboardingApi::class)

package com.android.onboarding.tasks.service

import android.content.Context
import android.os.PersistableBundle
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.OnboardingEvent
import com.android.onboarding.nodes.OnboardingGraphLog
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.nodes.UniqueNodeRef
import com.android.onboarding.process.KeepAliveManager
import com.android.onboarding.process.KeepAliveReason
import com.android.onboarding.tasks.ERROR_METADATA_CANNOT_BE_FOUND
import com.android.onboarding.tasks.ERROR_TASK_CANNOT_BE_FOUND
import com.android.onboarding.tasks.ERROR_TASK_CANNOT_BE_INTERRUPTED
import com.android.onboarding.tasks.ErasedOnboardingTaskToken
import com.android.onboarding.tasks.OnboardingTask
import com.android.onboarding.tasks.OnboardingTaskContract
import com.android.onboarding.tasks.OnboardingTaskManager
import com.android.onboarding.tasks.OnboardingTaskState
import com.android.onboarding.tasks.OnboardingTaskStateManager
import com.android.onboarding.tasks.OnboardingTaskToken
import com.android.onboarding.tasks.OnboardingTaskWithSynchronousExecution
import com.android.onboarding.tasks.ProviderManager
import com.android.onboarding.tasks.UnresolvedOnboardingTaskToken
import com.android.onboarding.tasks.security.TaskPermissionManager
import com.android.onboarding.tasks.utils.TaskResult
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.versions.OnboardingChanges
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.properties.ReadOnlyProperty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Implementation of [OnboardingTaskService] to manage all cross process requests related to task
 * execution.
 *
 * Clients should expose this binder, e.g. via a service, that allows clients to interact with the
 * onboarding task manager by binding to it and invoking tasks.
 *
 * Implementations are expected to bind it to the intent specified in
 * [OnboardingTaskContract.taskServiceIntent][com.android.onboarding.tasks.OnboardingTaskContract.taskServiceIntent].
 *
 * This class also maintains the task states for the tasks that it has executed by itself.
 *
 * @param component The [ComponentRef] of the component hosting this binder.
 * @param tasks A collection of suppliers for all tasks supported by this service binder. The
 *   supplier must supply a new instance of the tasks. Otherwise starting the same task multiple
 *   times will result in failures.
 * @param coroutineScope The [CoroutineScope] to be used for background task execution.
 * @see com.android.onboarding.tasks.OnboardingTaskManagerServiceBinder
 */
@OptIn(InternalOnboardingApi::class)
class OnboardingTaskServiceServer(
  component: ComponentRef,
  private val contextProvider: Supplier<Context>,
  private val tasks: Collection<Supplier<out OnboardingTask<*, *, *>>>,
  private val taskPermissionManager: TaskPermissionManager,
  private val keepAliveManager: KeepAliveManager,
  private val coroutineScope: CoroutineScope,
  private val onboardingChangesSupplier: Supplier<OnboardingChanges>,
  private val taskStateManager: OnboardingTaskStateManager,
  private val providedActivities: Set<OnboardingActivityApiContract<*, *>> = setOf(),
  private val providedTasks: Set<OnboardingTaskContract<*, *>> = setOf(),
  otm: ReadOnlyProperty<Any?, OnboardingTaskManager>,
) : OnboardingTaskService, ComponentRef by component {
  private val onboardingChanges
    get() = onboardingChangesSupplier.get()

  private val applicationContext
    get() = contextProvider.get().applicationContext

  private val otm by otm

  /**
   * A read-only registry of all the task definitions this manager understands. The registry is
   * indexed by the [OnboardingTaskContract] associated with each task.
   *
   * Additionally, relevant [ProviderTasks][com.android.onboarding.tasks.ProviderTask] are injected
   * as well.
   */
  private val taskRegistry: Map<NodeRef.Stable, Supplier<out OnboardingTask<*, *, *>>> by lazy {
    val providers =
      ProviderManager.buildProviders(
        component = asComponentRef(),
        tasks = providedTasks,
        activities = providedActivities,
      )
    ConcurrentHashMap(tasks.associateBy { it.get().contract.asNodeRef() } + providers)
  }

  override suspend fun startTask(token: OnboardingTaskToken<*>, input: PersistableBundle) =
    executeServerAction(token, onError = { logTaskCompletion(taskStateManager[token]) }) {
      Log.v(TAG, "startTask[$token]")
      taskPermissionManager.enforceCallerPermission(applicationContext)
      val task = createTask(token)
      taskStateManager.initMetadataChannel(token)

      taskStateManager.cacheTaskInstance(token, task)
      val resolvedToken = token.resolve(task.contract)
      // Update the task state as in progress immediately before running the task.
      taskStateManager[token] = OnboardingTaskState.InProgress(resolvedToken)


      val keepAliveHandle = keepAliveManager.keepAlive(KeepAliveReason.OTHER)
      val startDeferred = CompletableDeferred<Unit>()
      val job =
        executeTask(
          token = token,
          task = task,
          input = input,
          onStart = { startDeferred.complete(Unit) },
          onFinish = {
            keepAliveHandle.close()
            taskStateManager.closeMetadataChannel(token)
            task.onDestroy()
          },
        )
      // Cache the execution so that we can get back to it later.
      taskStateManager.cacheTaskExecution(token, job)
      Log.v(TAG, "startTask: Cached task execution for $token")
      startDeferred.await()
      taskStateManager.flush()
    }

  override suspend fun <O> getTaskState(token: OnboardingTaskToken<O>): OnboardingTaskState<O> =
    executeServerAction(token) {
      Log.d(TAG, "Getting task state for node ${token.identity()}")
      taskPermissionManager.enforceCallerPermission(applicationContext)
      taskStateManager.restoreTaskStatesIfNecessary()
      taskStateManager[token]
    }

  /**
   * Gets task state for [token] directly without any validation or exception handling.
   *
   * @see getTaskState
   */
  @InternalOnboardingApi
  fun <O> getTaskStateDirectly(token: OnboardingTaskToken<O>): OnboardingTaskState<O>? {
    return taskStateManager.getOrNull(token)
  }

  override fun <O> observeTaskState(token: OnboardingTaskToken<O>): Flow<OnboardingTaskState<O>> =
    flow {
        executeServerAction(
          token,
          onError = { Log.w(TAG, "Failure observing task state for node ${token.identity()}", it) },
        ) {
          taskPermissionManager.enforceCallerPermission(applicationContext)
          taskStateManager.restoreTaskStatesIfNecessary()

          val currentState = taskStateManager[token]
          if (taskStateManager[token].isCompleted) {
            Log.d(TAG, "Task($token) is already complete. Closing state observer.")
            emit(currentState)
            return@flow
          }

          val stateFlow = taskStateManager.observeState(token)
          if (stateFlow == null) {
            Log.w(TAG, "State flow not found for node ${token.identity()}.")
            throw IllegalStateException(ERROR_TASK_CANNOT_BE_FOUND)
          } else {
            emit(currentState)
            // Dropping current state as we have already published that.
            stateFlow.drop(1).collect { state ->
              Log.d(TAG, "Emitting state change for node ${token.identity()}: $state")
              emit(state.resolve(token))
            }
          }
        }
      }
      .distinctUntilChanged()

  override suspend fun getTaskMetadata(token: OnboardingTaskToken<*>): OnboardingMetadataStore =
    executeServerAction(
      token,
      onError = { Log.w(TAG, "Failure getting task metadata for node ${token.identity()}", it) },
    ) {
      taskPermissionManager.enforceCallerPermission(applicationContext)
      taskStateManager.getMetadata(token)
    }

  override fun observeTaskMetadata(token: OnboardingTaskToken<*>): Flow<OnboardingMetadataStore> =
    flow {
        executeServerAction(
          token,
          onError = {
            Log.w(TAG, "Failure observing task metadata for node ${token.identity()}", it)
          },
        ) {
          taskPermissionManager.enforceCallerPermission(applicationContext)
          taskStateManager.restoreTaskStatesIfNecessary()

          val initialState = taskStateManager[token]
          if (taskStateManager[token].isCompleted) {
            Log.d(TAG, "Task($token) is already complete. Closing metadata observer.")
            emit(initialState.lastMetadata ?: OnboardingMetadataStore())
            return@flow
          }

          val metadataFlow = taskStateManager.observeMetadata(token)
          if (metadataFlow == null) {
            Log.w(TAG, "Metadata flow not found for node ${token.identity()}.")
            throw IllegalStateException(ERROR_METADATA_CANNOT_BE_FOUND)
          } else {
            emit(initialState.lastMetadata ?: OnboardingMetadataStore())
            metadataFlow.collect { metadata ->
              Log.d(TAG, "Emitting metadata change for node ${token.identity()}: $metadata")
              emit(metadata)
            }
          }
        }
      }
      .distinctUntilChanged()

  override suspend fun interruptTask(token: OnboardingTaskToken<*>, reason: InterruptReason) =
    executeServerAction(
      token,
      onError = { Log.w(TAG, "Failure interrupting task for node ${token.identity()}", it) },
    ) {
      taskPermissionManager.enforceCallerPermission(applicationContext)
      Log.w(TAG, "Interrupting node ${token.identity()} with reason $reason")
      val task = taskStateManager.getTaskInstance(token)
      if (task == null) {
        Log.e(TAG, "No instance found for interrupt to token ${token.identity()}")
        throw IllegalStateException(ERROR_TASK_CANNOT_BE_FOUND)
      } else if (!task.contract.canAcceptInterrupt(reason)) {
        Log.d(TAG, "Task ${task.contract} does not accept interrupt $reason")
        throw IllegalStateException(ERROR_TASK_CANNOT_BE_INTERRUPTED)
      } else {
        task.onInterrupt(reason)
      }
    }

  override suspend fun <O> awaitTaskCompletion(
    token: OnboardingTaskToken<O>
  ): OnboardingTaskState.Completed<O> =
    executeServerAction(
      token,
      onError = { Log.w(TAG, "Failure awaiting task completion for node ${token.identity()}", it) },
    ) {
      Log.v(TAG, "awaitTaskCompletion($token)")
      taskPermissionManager.enforceCallerPermission(applicationContext)

      taskStateManager.restoreTaskStatesIfNecessary()
      val currentState: OnboardingTaskState<O> = taskStateManager[token]
      // We need to check against the ErasedOnboardingState in case the state is unresolved. This
      // is the case when the state was persisted in disk. The contract can't be persisted, hence
      // can't be resolved.
      val state =
        if (currentState.isCompleted) {
          Log.v(TAG, "awaitTaskCompletion: Task($token) is already completed - $currentState")
          currentState
        } else {
          // Waiting for task to complete.
          checkNotNull(taskStateManager.getTaskExecution(token)) { ERROR_TASK_CANNOT_BE_FOUND }
            .join()
          taskStateManager[token]
        }
      Log.v(TAG, "awaitTaskCompletion($token): Flushing before returning")
      taskStateManager.flush()
      // We resolve with token again even though we have already have <O> return type.
      // This ensures that even recovered ErasedOnboardingState still returns correct result types.
      return state.resolve(token) as OnboardingTaskState.Completed<O>
    }

  // -----------------------------------------------------------------------------------------------

  /**
   * Creates and initializes a task given a [token] without running it.
   *
   * This method is side-effect free and all stateful interactions related to the returned task
   * instance should be done by the caller.
   */
  private fun createTask(token: UnresolvedOnboardingTaskToken<*>): OnboardingTask<*, *, *> {
    Log.v(TAG, "Creating task for node ${token.identity()}")
    val taskSupplier = taskRegistry[token.asNodeRef()]
    val task =
      if (taskSupplier == null) {
        Log.w(TAG, "Task for node $token not found!")
        error(ERROR_TASK_CANNOT_BE_FOUND)
      } else {
        taskSupplier.get()
      }

    Log.v(TAG, "Initialising task instance token=$token task=$task")
    if (task.isCreated) {
      Log.wtf(TAG, "Task ($task) was already created. Please don't reuse the same task instance.")
      // TaskSupplier doesn't always return a new task instance. It can return a singleton.

      // Returning [null] will result in a task not found error message for the latest execution.
      // Returning [task] will reuse the old task. The first execution will fail due to bad states.
      // The latter is worse because the error logs are more confusing.
      error("Attempted to reuse the same task instance.")
    }
    task.onCreate(
      OnboardingTask.OnboardingTaskContext(
        nodeRef = token,
        taskStateManager = taskStateManager,
        coroutineContext = coroutineScope.coroutineContext,
        onboardingChanges = onboardingChanges,
        onboardingTaskManager = otm,
      )
    )
    Log.d(TAG, "Task initialised for node ${token.identity()}.")
    return task
  }

  /**
   * Execute the task asynchronously in the current process.
   *
   * @return a [Job] instance of this execution.
   */
  @CanIgnoreReturnValue
  private fun executeTask(
    token: UnresolvedOnboardingTaskToken<*>,
    task: OnboardingTask<*, *, *>,
    input: PersistableBundle,
    onStart: () -> Unit,
    onFinish: () -> Unit,
  ): Job =
    coroutineScope.launch {
      logTaskStart(task, input)
      onStart()
      try {
        // Execute the task and await its completion.
        Log.v(TAG, "Starting task execution for node ${token.identity()}")
        val taskState = task.execute(context = applicationContext, input = input)
        Log.d(TAG, "Successfully executed task for node ${token.identity()}: state=$taskState")

        // Update the tasksStates map with the actual task result after completion.
        taskStateManager[token] = taskState
        taskStateManager.flush()
        logTaskCompletion(taskState)
      } catch (e: Exception) {
        Log.w(TAG, "Task execution for node ${token.identity()} failed.", e)
        logTaskCompletion(taskStateManager[token], e)
        if (e is CancellationException) throw e
      } finally {
        onFinish()
      }
    }

  /** Logs [task] start event to [OnboardingGraphLog]. */
  private fun logTaskStart(task: OnboardingTask<*, *, *>, input: PersistableBundle) {
    OnboardingGraphLog.log(
      OnboardingEvent.OnboardingTaskStartedEvent(

        //   (when .runTask is called)
        sourceNodeId = UNKNOWN_NODE_ID,
        nodeName = task.nodeName,
        nodeComponent = task.nodeComponent,
        nodeId = task.nodeId,
        argument =
          try {
            task.contract.inputDecoder.encode(input)
          } catch (_: Exception) {
            // this likely means the task is being started with some other input (e.g. chained
            // contracts)
            input
          },
      )
    )
  }

  /** Logs completed [state] to [OnboardingGraphLog]. */
  private fun logTaskCompletion(state: OnboardingTaskState<*>, throwable: Throwable? = null) {
    val result =
      when (state) {
        is OnboardingTaskState.Success<*> ->
          state.result as? TaskResult<*> ?: TaskResult.Success(state.result)

        is OnboardingTaskState.Failure<*> ->
          state.result as? TaskResult<*>
            ?: TaskResult.Failure<Any>(throwable = throwable, failureReason = state.errorMessage)

        is OnboardingTaskState.InProgress<*> -> {
          Log.wtf(TAG, "logCompleteTask called with non-complete task ${state.identity()} ($state)")
          return
        }
      }
    OnboardingGraphLog.log(
      OnboardingEvent.OnboardingTaskCompleteEvent(
        nodeName = state.nodeName,
        nodeComponent = state.nodeComponent,
        nodeId = state.nodeId,
        result = result,
      )
    )
  }

  /**
   * Executes [action] and wraps all exceptions except [CancellationException] into
   * [OnboardingTaskServiceServerException].
   *
   * @param action The action to execute.
   * @param onError Optional callback to be invoked on any errors (including
   *   [CancellationException]).
   */
  private suspend inline fun <R> executeServerAction(
    ref: UniqueNodeRef,
    onError: (Throwable) -> Unit = {},
    action: suspend () -> R,
  ) =
    try {
      action()
    } catch (e: CancellationException) {
      onError(e)
      throw e
    } catch (e: OnboardingTaskServiceServerException) {
      onError(e)
      throw e
    } catch (e: Throwable) {
      Log.w(TAG, "Server action failed", e)
      onError(e)
      throw OnboardingTaskServiceServerException(ref, e)
    }

  /**
   * True if the task is registered with this binder and it can be executed synchronously at least
   * some of the time.
   */
  fun isSynchronousTask(contract: NodeRef) =
    taskRegistry[contract.asNodeRef()]?.get() is OnboardingTaskWithSynchronousExecution<*, *, *>

  /**
   * Run the task referenced by [token] only if that task can be executed entirely synchronously.
   *
   * If it can, then true will be returned. Otherwise false will be returned and the state will not
   * be changed. Results should be fetched in the usual way.
   */
  fun runTaskSynchronous(token: ErasedOnboardingTaskToken, bundle: PersistableBundle): Boolean {
    val task =
      try {
        createTask(token)
      } catch (e: Exception) {
        Log.e(TAG, "Could not initialise synchronous task for token $token", e)
        return false
      }
    if (task !is OnboardingTaskWithSynchronousExecution<*, *, *>) {
      Log.e(TAG, "Task for token $token is not synchronous")
      return false
    }

    val state = task.runTaskSynchronous(context = applicationContext, input = bundle)
    if (state == null) {
      Log.w(TAG, "Synchronous task $task is not run synchronously")
      return false
    }

    taskStateManager[token] = state
    return true
  }

  private companion object {
    const val TAG = "OnboardingTaskServiceServer"
  }
}
