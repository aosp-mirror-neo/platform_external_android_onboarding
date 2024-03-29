package com.android.onboarding.tasks

import android.content.Context
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch

class DefaultOnboardingTaskManager
private constructor(private val appContext: Context, private val coroutineScope: CoroutineScope) :
  OnboardingTaskManager {

  private val tasksStates = ConcurrentHashMap<OnboardingTaskToken, OnboardingTaskState<*>>()

  override fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
    TaskT : OnboardingTask<TaskArgsT, TaskResultT, TaskContractT>,
  > runTask(taskContract: TaskContractT, task: TaskT, taskArgs: TaskArgsT): OnboardingTaskToken {
    val taskToken =
      OnboardingTaskToken(task::class.java.simpleName, taskContract::class.java.simpleName)
    // We have to update the task status as soon as possible to prevent immediate query status
    // action.
    tasksStates[taskToken] = OnboardingTaskState.InProgress<TaskResultT>()

    // Run the task asynchronously.
    coroutineScope.launch { performTask(taskContract, task, taskArgs, taskToken) }

    Log.d(TAG, "runTask#Return task token immediately.")
    return taskToken
  }

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
    task: OnboardingTask<TaskArgsT, TaskResultT, TaskContractT>,
    taskArgs: TaskArgsT,
  ): ListenableFuture<OnboardingTaskState<TaskResultT>> {
    return coroutineScope.future { runTaskAndGetResult(taskContract, task, taskArgs) }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <TaskResultT> getTaskState(
    taskToken: OnboardingTaskToken
  ): OnboardingTaskState<TaskResultT>? {
    // This method performs a cast from a wildcard type (`OnboardingTaskState<*>) to the expected
    // result type (`OnboardingTaskState<Result>`). The cast is unchecked due to type erasure at
    // runtime, and the `as?` operator is used to safely handle cases where the cast might fail,
    // resulting in a null value.
    return tasksStates[taskToken] as? OnboardingTaskState<TaskResultT>
  }

  override suspend fun <TaskResultT> waitForCompleted(
    taskToken: OnboardingTaskToken
  ): OnboardingTaskState<TaskResultT>? {
    while (true) {
      val currentState: OnboardingTaskState<TaskResultT>? = getTaskState(taskToken)
      when (currentState) {
        null -> return null
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
  ): ListenableFuture<OnboardingTaskState<TaskResultT>?> {
    return coroutineScope.future { waitForCompleted(taskToken) }
  }

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

  /**
   * Gets an instance of [DefaultOnboardingTaskManager], creating one if it does not exist.
   *
   * @return The singleton instance of [DefaultOnboardingTaskManager].
   */
  companion object {

    const val TAG: String = "DefaultOTM"
    private var instance: DefaultOnboardingTaskManager? = null

    /**
     * Gets an instance of [DefaultOnboardingTaskManager], creating one if it does not exist. A
     * default coroutine scope with [Dispatchers.Default] and [SupervisorJob] will be used for this
     * function.
     *
     * @param appContext The application context.
     * @return The singleton instance of [DefaultOnboardingTaskManager].
     */
    fun getInstance(appContext: Context): DefaultOnboardingTaskManager {
      return getInstance(appContext, CoroutineScope(Dispatchers.Default + SupervisorJob()))
    }

    /**
     * Gets an instance of [DefaultOnboardingTaskManager], creating one if it does not exist. You
     * can customize the coroutine scope by providing your own [CoroutineScope] instance. If not
     * provided, a default coroutine scope with [Dispatchers.Default] and [SupervisorJob] will be
     * used.
     *
     * @param appContext The application context.
     * @param coroutineScope The optional [CoroutineScope] for custom coroutine handling.
     * @return The singleton instance of [DefaultOnboardingTaskManager].
     */
    fun getInstance(
      appContext: Context,
      coroutineScope: CoroutineScope? = null,
    ): DefaultOnboardingTaskManager {
      return instance
        ?: synchronized(this) {
          instance
            ?: DefaultOnboardingTaskManager(
                appContext,
                coroutineScope ?: CoroutineScope(Dispatchers.Default + SupervisorJob()),
              )
              .also { instance = it }
        }
    }

    fun release() {
      instance?.coroutineScope?.cancel()
      instance = null
    }
  }
}
