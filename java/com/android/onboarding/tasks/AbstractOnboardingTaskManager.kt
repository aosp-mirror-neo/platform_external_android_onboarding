package com.android.onboarding.tasks

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.tasks.crossApp.CrossProcessTaskManager
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
  private val taskStateManager = OnboardingTaskStateManager()

  init {
    // Initialize the mapping between task contracts and tasks.
    contractAndTaskMap = initializeContractAndTaskMap()
  }

  /**
   * Assign a component name for this task manager. The component name must reference an
   * [OnboardingComponents] constant.
   */
  abstract val componentName: String

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
    val taskToken: OnboardingTaskToken

    if (isTaskRunInSameProcess(taskContract)) {
      Log.i(TAG, "Run task: $taskContract in same process.")
      val task =
        tryCreateTaskInstance(taskContract::class.java) ?: return OnboardingTaskToken.INVALID
      taskToken = OnboardingTaskToken(taskContract::class.java.name, taskContract.componentName)
      // Update the task state as in progress immediately before running the task.
      taskStateManager.updateTaskState(taskToken, OnboardingTaskState.InProgress<Nothing>())
      // Run the task asynchronously.
      coroutineScope.launch { performTask(taskContract, task, taskArgs, taskToken) }
    } else {
      Log.i(TAG, "Run task: $taskContract in cross process.")
      // Cross process triggers task asynchronously.
      taskToken =
        CrossProcessTaskManager.getInstance(appContext, taskStateManager)
          .runTask(taskContract, taskArgs)
      // Mark the task in progress.
      taskStateManager.updateTaskState(taskToken, OnboardingTaskState.InProgress<Nothing>())
    }

    Log.d(TAG, "Return task token immediately.")
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
    val taskToken = OnboardingTaskToken(taskContract::class.java.name, taskComponentName = "")
    // We have to update the task status as soon as possible to prevent immediate query status
    // action.
    taskStateManager.updateTaskState(taskToken, OnboardingTaskState.InProgress<Nothing>())

    // Execute the task and await its completion.
    performTask(taskContract, task, taskArgs, taskToken)

    // Because task state includes different types of results in the list.
    return getTaskState(taskToken)
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
    val taskToken = OnboardingTaskToken(taskContract::class.java.name, taskComponentName = "")
    // We have to update the task status as soon as possible to prevent immediate query status
    // action.
    taskStateManager.updateTaskState(taskToken, OnboardingTaskState.InProgress<Nothing>())

    // Execute the task and await its completion.
    performTask(taskContract, task, taskArgs, taskToken)

    return getTaskState(taskToken)
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

  override fun <TaskResultT> getTaskState(
    taskToken: OnboardingTaskToken
  ): OnboardingTaskState<TaskResultT> {
    return taskStateManager.getTaskState(taskToken)
  }

  override suspend fun <TaskResultT> waitForCompleted(
    taskToken: OnboardingTaskToken
  ): OnboardingTaskState<TaskResultT> {
    while (true) {
      val currentState = getTaskState<TaskResultT>(taskToken)
      Log.d(TAG, "waitForCompleted#currentState: $currentState")
      when (currentState) {
        is OnboardingTaskState.Completed<*>,
        is OnboardingTaskState.Failed<*> -> return currentState
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
  ): ListenableFuture<OnboardingTaskState<TaskResultT>> =
    coroutineScope.future { waitForCompleted(taskToken) }

  override fun getContractAndTaskMap():
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
    val taskState = task.runTask(taskContract, taskArgs)

    Log.d(TAG, "performTask#end")

    // Update the tasksStates map with the actual task result after completion.
    taskStateManager.updateTaskState(taskToken, taskState)
  }

  private fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > tryCreateTaskInstance(
    contractClass: Class<out TaskContractT>
  ): OnboardingTask<TaskArgsT, TaskResultT, TaskContractT>? {
    val taskClass = contractAndTaskMap[contractClass] ?: return null

    try {
      val constructor = taskClass.getDeclaredConstructor(Context::class.java)
      // Create a new instance of the contract class using the constructor
      @Suppress("UNCHECKED_CAST")
      return constructor.newInstance(appContext)
        as? OnboardingTask<TaskArgsT, TaskResultT, TaskContractT>
    } catch (e: Exception) {
      Log.w(TAG, "Error instantiating task: $e")
    }
    return null
  }

  private fun isTaskRunInSameProcess(contract: OnboardingTaskContract<*, *>): Boolean {
    val contractComponentName = OnboardingNode.extractComponentNameFromClass(contract::class.java)
    return TextUtils.equals(componentName, "DefaultOnboardingTaskManager") ||
      TextUtils.equals(componentName, contractComponentName)
  }

  companion object {
    private const val TAG: String = "OTMBase"
  }
}
