package com.android.onboarding.tasks

/**
 * Represents the state of an onboarding task.
 *
 * @param Result The type of the result associated with the task.
 */
sealed class OnboardingTaskState<Result> {

  /**
   * Represents the in-progress state of an onboarding task.
   *
   * @param result The current result, if available.
   */
  data class IN_PROGRESS<Result>(var result: Result? = null) : OnboardingTaskState<Result>()

  /**
   * Represents the completed state of an onboarding task.
   *
   * @param result The result of the completed task.
   */
  data class COMPLETED<Result>(val result: Result? = null) : OnboardingTaskState<Result>()

  /**
   * Represents the failed state of an onboarding task.
   *
   * @param errorMessage The error message describing the failure.
   * @param result The result associated with the failed task, if available.
   */
  data class FAILED<Result>(val errorMessage: String, val result: Result? = null) :
    OnboardingTaskState<Result>()
}
