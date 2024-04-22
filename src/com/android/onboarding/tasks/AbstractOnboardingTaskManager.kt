package com.android.onboarding.tasks

import android.content.Context
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch

/**
 * Base class for managing the execution and state of onboarding tasks within the onboarding
 * process. This class provides common part of implementation for triggering tasks, monitoring their
 * progress, and obtaining results from onboarding tasks.
 */
abstract class AbstractOnboardingTaskManager(
  protected val appContext: Context,
  protected val coroutineScope: CoroutineScope,
) : OnboardingTaskManager {

  // Mapping between onboarding task contracts and corresponding tasks.
  private val contractAndTaskMap:
    ConcurrentHashMap<Class<out OnboardingTaskContract<*, *>>, Class<out OnboardingTask<*, *, *>>>
  private val tasksStates = ConcurrentHashMap<OnboardingTaskToken, OnboardingTaskState<*>>()

  init {
    // Initialize the mapping between task contracts and tasks.
    contractAndTaskMap = initializeContractAndTaskMap()
  }

  /**
   * Initializes a mapping between onboarding task contracts and corresponding onboarding tasks.
   * This method should be overridden by implementing classes to provide custom mappings between
   * specific onboarding task contracts and their corresponding task implementations for their
   * application process.
   *
   * @return A map where the keys represent classes implementing the [OnboardingTaskContract], and
   *   the values represent classes implementing the [ OnboardingTask]. The mapping specifies the
   *   relationship between task contracts and their associated tasks. Implementing classes should
   *   populate this map with their desired mappings.
   */
  abstract fun initializeContractAndTaskMap():
    ConcurrentHashMap<Class<out OnboardingTaskContract<*, *>>, Class<out OnboardingTask<*, *, *>>>

  override fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTask(taskContract: TaskContractT, taskArgs: TaskArgsT): OnboardingTaskToken {
    val task = tryCreateTaskInstance(taskContract::class.java) ?: return OnboardingTaskToken.INVALID
    val taskToken =
      OnboardingTaskToken(task::class.java.simpleName, taskContract::class.java.simpleName)
    // We have to update the task status as soon as possible to prevent immediate query status
    // action.
    tasksStates[taskToken] = OnboardingTaskState.InProgress<TaskResultT>()

    // Run the task asynchronously.
    coroutineScope.launch { performTask(taskContract, task, taskArgs, taskToken) }

    Log.d(TAG, "runTask() - Return task token immediately.")
    return taskToken
  }

  override suspend fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTaskAndGetResult(
    taskContract: TaskContractT,
    taskArgs: TaskArgsT,
  ): OnboardingTaskState<TaskResultT> {
    val task =
      tryCreateTaskInstance(taskContract::class.java)
        ?: return OnboardingTaskState.Failed(ERROR_INSTANTIATING_TASK)
    val taskToken =
      OnboardingTaskToken(task::class.java.simpleName, taskContract::class.java.simpleName)
    // We have to update the task status as soon as possible to prevent immediate query status
    // action.
    tasksStates[taskToken] = OnboardingTaskState.InProgress<TaskResultT>()

    // Execute the task and await its completion.
    performTask(taskContract, task, taskArgs, taskToken)

    return getTaskState(taskToken)
      ?: error(
        "Unexpected: No task result retrieved. You must see a task result when developer calls the suspend [runTaskAndGetResult]."
      )
  }

  @Deprecated("Use new overload function - runTaskAndGetResult().")
  override suspend fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTaskAndGetResult(
    taskContract: TaskContractT,
    task: OnboardingTask<TaskArgsT, TaskResultT, TaskContractT>,
    taskArgs: TaskArgsT,
  ): OnboardingTaskState<TaskResultT> {
    val taskToken =
      OnboardingTaskToken(task::class.java.simpleName, taskContract::class.java.simpleName)
    // We have to update the task status as soon as possible to prevent immediate query status
    // action.
    tasksStates[taskToken] = OnboardingTaskState.InProgress<TaskResultT>()

    // Execute the task and await its completion.
    performTask(taskContract, task, taskArgs, taskToken)

    return getTaskState(taskToken)
      ?: error(
        "Unexpected: No task result retrieved. You must see a task result when developer calls the suspend [runTaskAndGetResult]."
      )
  }

  override fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTaskAndGetResultAsync(
    taskContract: TaskContractT,
    taskArgs: TaskArgsT,
  ): ListenableFuture<OnboardingTaskState<TaskResultT>> {
    return coroutineScope.future { runTaskAndGetResult(taskContract, taskArgs) }
  }

  @Deprecated("Use new overload function - runTaskAndGetResultAsync().")
  override fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTaskAndGetResultAsync(
    taskContract: TaskContractT,
    task: OnboardingTask<TaskArgsT, TaskResultT, TaskContractT>,
    taskArgs: TaskArgsT,
  ): ListenableFuture<OnboardingTaskState<TaskResultT>> {
    return coroutineScope.future { runTaskAndGetResult(taskContract, task, taskArgs) }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <TaskResultT> getTaskState(
    taskToken: OnboardingTaskToken
  ): OnboardingTaskState<TaskResultT> {
    // This method performs a cast from a wildcard type (`OnboardingTaskState<*>) to the expected
    // result type (`OnboardingTaskState<Result>`). The cast is unchecked due to type erasure at
    // runtime, and the `as?` operator is used to safely handle cases where the cast might fail,
    // resulting in a null value.
    return tasksStates[taskToken]?.let { taskState ->
      // Only attempt the cast if the value in the map is not null
      taskState as? OnboardingTaskState<TaskResultT>
    } ?: OnboardingTaskState.Failed(ERROR_TASK_CANT_BE_FOUND)
  }

  override suspend fun <TaskResultT> waitForCompleted(
    taskToken: OnboardingTaskToken
  ): OnboardingTaskState<TaskResultT> {
    while (true) {
      val currentState: OnboardingTaskState<TaskResultT> = getTaskState(taskToken)
      when (currentState) {
        is OnboardingTaskState.Completed,
        is OnboardingTaskState.Failed -> return currentState
        else -> {
          // Do nothing here as task is in progress.
        }
      }
      // Sleep for a short interval before checking again.
      Log.d(TAG, "waitForCompleted#sleep... 500 ms")
      delay(500)
    }
  }

  override fun <TaskResultT> waitForCompletedAsync(
    taskToken: OnboardingTaskToken
  ): ListenableFuture<OnboardingTaskState<TaskResultT>> {
    return coroutineScope.future { waitForCompleted(taskToken) }
  }

  fun getContractAndTaskMap():
    ConcurrentHashMap<Class<out OnboardingTaskContract<*, *>>, Class<out OnboardingTask<*, *, *>>> =
    contractAndTaskMap

  private suspend fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > performTask(
    taskContract: TaskContractT,
    task: OnboardingTask<TaskArgsT, TaskResultT, TaskContractT>,
    taskArgs: TaskArgsT,
    taskToken: OnboardingTaskToken,
  ) {
    Log.d(TAG, "performTask#start")

    // Validate all inputs by the defined contract.
    taskContract.validate(taskArgs)

    // Execute the task and await its completion.
    val taskResult = task.runTask(taskContract, taskArgs)

    Log.d(TAG, "performTask#end")

    // Update the tasksStates map with the actual task result after completion.
    tasksStates[taskToken] = taskResult
  }

  private fun <
    TaskArgs,
    TaskResult,
    TaskContract : OnboardingTaskContract<TaskArgs, TaskResult>,
  > tryCreateTaskInstance(
    contractClass: Class<out TaskContract>
  ): OnboardingTask<TaskArgs, TaskResult, TaskContract>? {
    val taskClass = contractAndTaskMap[contractClass] ?: return null

    try {
      val constructor = taskClass.getDeclaredConstructor(Context::class.java)
      // Create a new instance of the contract class using the constructor
      @Suppress("UNCHECKED_CAST")
      return constructor.newInstance(appContext)
        as? OnboardingTask<TaskArgs, TaskResult, TaskContract>
    } catch (e: Exception) {
      Log.w(TAG, "Error instantiating task: $e")
    }
    return null
  }

  companion object {
    const val TAG: String = "OTMBase"
  }
}
