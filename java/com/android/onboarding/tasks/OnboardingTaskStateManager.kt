package com.android.onboarding.tasks

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the states of onboarding tasks in a centralized and thread-safe manner.
 *
 * [OnboardingTaskStateManager] allows you to track/update the progress and results of individual
 * onboarding tasks, identified by unique [OnboardingTaskToken] instances.
 */
class OnboardingTaskStateManager {

  private val taskStates = ConcurrentHashMap<OnboardingTaskToken, OnboardingTaskState<*>>()

  /**
   * Updates the state of an onboarding task.
   *
   * @param token The [OnboardingTaskToken] identifying the task.
   * @param state The updated [OnboardingTaskState] object representing the task's new state. The
   *   type of the result within the state object is flexible.
   */
  fun updateTaskState(token: OnboardingTaskToken, state: OnboardingTaskState<*>) {
    taskStates[token] = state
  }

  /**
   * Retrieves the current state of an onboarding task.
   *
   * @param token The [OnboardingTaskToken] identifying the task.
   * @return The [OnboardingTaskState] object associated with the task. If no state is found for the
   *   provided token, a [OnboardingTaskState.Failed] with the error message
   *   [ERROR_TASK_CANT_BE_FOUND] is returned.
   */
  fun <ResultT> getTaskState(token: OnboardingTaskToken): OnboardingTaskState<ResultT> {
    return taskStates[token] as? OnboardingTaskState<ResultT>
      ?: OnboardingTaskState.Failed<ResultT>(ERROR_TASK_CANT_BE_FOUND)
  }
}
