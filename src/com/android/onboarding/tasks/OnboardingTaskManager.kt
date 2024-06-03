package com.android.onboarding.tasks

import com.google.common.util.concurrent.ListenableFuture

/**
 * Manages the execution and state of onboarding tasks within the onboarding process. This interface
 * provides a set of functions for triggering a task, monitoring, and obtaining results from
 * onboarding tasks.
 */
interface OnboardingTaskManager {

  /**
   * Executes an onboarding task asynchronously and returns a token for subsequent status queries.
   *
   * @param taskContract The contract defining the task's arguments and result for the onboarding
   *   task.
   * @return The token associated with the initiated onboarding task.
   */
  fun <TaskResultT, TaskContractT : OnboardingTaskContract<Unit, TaskResultT>> runTask(
    taskContract: TaskContractT
  ): OnboardingTaskToken = runTask(taskContract, Unit)

  /**
   * Executes an onboarding task asynchronously and returns a token for subsequent status queries.
   *
   * @param taskContract The contract defining task's arguments and result for the onboarding task.
   * @param taskArgs A defined argument for the onboarding task.
   * @return The token associated with the initiated onboarding task.
   */
  fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTask(taskContract: TaskContractT, taskArgs: TaskArgsT): OnboardingTaskToken

  /**
   * Executes an onboarding task asynchronously and waits for its completion, providing the final
   * result.
   *
   * @param taskContract The contract defining task's arguments and result for the onboarding task.
   * @param taskArgs A defined argument for the onboarding task.
   * @return The final result of the onboarding task.
   */
  suspend fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTaskAndGetResult(
    taskContract: TaskContractT,
    taskArgs: TaskArgsT,
  ): OnboardingTaskState<TaskResultT>

  /**
   * Executes an onboarding task asynchronously and waits for its completion, providing the final
   * result.
   *
   * @param taskContract The contract defining task's arguments and result for the onboarding task.
   * @param task The onboarding task to be executed.
   * @param taskArgs A defined argument for the onboarding task.
   * @return The final result of the onboarding task.
   */
  @Deprecated("Use new overload function - runTaskAndGetResult().")
  suspend fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTaskAndGetResult(
    taskContract: TaskContractT,
    task: OnboardingTask<TaskArgsT, TaskResultT, TaskContractT>,
    taskArgs: TaskArgsT,
  ): OnboardingTaskState<TaskResultT>

  /**
   * Executes an onboarding task asynchronously and waits for its completion, providing the final
   * result.
   *
   * @param taskContract The contract defining task's arguments and result for the onboarding task.
   * @param taskArgs A defined argument for the onboarding task.
   * @return A [ListenableFuture] representing the final state of the onboarding task. The future
   *   encapsulates the asynchronous execution and completion of the task, allowing for the
   *   retrieval of the task state or result.
   */
  fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTaskAndGetResultAsync(
    taskContract: TaskContractT,
    taskArgs: TaskArgsT,
  ): ListenableFuture<OnboardingTaskState<TaskResultT>>

  /**
   * Executes an onboarding task asynchronously and waits for its completion, providing the final
   * result.
   *
   * @param taskContract The contract defining task's arguments and result for the onboarding task.
   * @param task The onboarding task to be executed.
   * @param taskArgs A defined argument for the onboarding task.
   * @return A [ListenableFuture] representing the final state of the onboarding task. The future
   *   encapsulates the asynchronous execution and completion of the task, allowing for the
   *   retrieval of the task state or result.
   */
  @Deprecated("Use new overload function - runTaskAndGetResultAsync().")
  fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTaskAndGetResultAsync(
    taskContract: TaskContractT,
    task: OnboardingTask<TaskArgsT, TaskResultT, TaskContractT>,
    taskArgs: TaskArgsT,
  ): ListenableFuture<OnboardingTaskState<TaskResultT>>

  /**
   * Retrieves the current state of a previously initiated onboarding task.
   *
   * @param taskToken The token associated with the onboarding task.
   * @return The result of the onboarding task.
   */
  fun <TaskResultT> getTaskState(taskToken: OnboardingTaskToken): OnboardingTaskState<TaskResultT>

  /**
   * Waits for the result of a previously initiated onboarding task.
   *
   * If the provided [taskToken] is not valid, the caller receives a null object, indicating that
   * this token is not recorded in the manager.
   *
   * If the [taskToken] represents a task in a timeout state or encounters an exeception, the caller
   * receives a [OnboardingTaskState.Failed] state.
   *
   * If the onboarding task completes successfully, the caller receives a
   * [OnboardingTaskState.Completed] state with [Result] data.
   *
   * @param taskToken The token associated with the onboarding task.
   * @return The final result of the onboarding task.
   */
  suspend fun <TaskResultT> waitForCompleted(
    taskToken: OnboardingTaskToken
  ): OnboardingTaskState<TaskResultT>

  /**
   * Asynchronously waits for the result of a previously initiated onboarding task.
   *
   * If the provided [taskToken] is not valid, the caller receives a null object, indicating that
   * this token is not recorded in the manager.
   *
   * If the [taskToken] represents a task in a timeout state or encounters an exeception, the caller
   * receives a [OnboardingTaskState.Failed] state.
   *
   * If the onboarding task completes successfully, the caller receives a
   * [OnboardingTaskState.Completed] state with [Result] data.
   *
   * @param taskToken The token associated with the onboarding task.
   * @return A [ListenableFuture] containing the final result of the onboarding task.
   */
  fun <TaskResultT> waitForCompletedAsync(
    taskToken: OnboardingTaskToken
  ): ListenableFuture<OnboardingTaskState<TaskResultT>>
}
