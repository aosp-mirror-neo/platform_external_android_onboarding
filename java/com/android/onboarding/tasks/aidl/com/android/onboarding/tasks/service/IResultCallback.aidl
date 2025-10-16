package com.android.onboarding.tasks.service;

import com.android.onboarding.tasks.service.IPersistableBundleStream;

/**
 * A callback to be invoked when the task completes.
 */
interface IResultCallback {
  /**
   * Version of this implementation to know which methods are available.
   *
   * @since 1
   */
  int version() = 0;

  /**
   * Method invoked on successful completion of the task.
   *
   * @param state A serialised instance of OnboardingTaskState.Completed.
   * @since 1
   */
  oneway void onSuccess(in IPersistableBundleStream result) = 1;

  /**
   * Method invoked on task error.
   *
   * Note that error in this context refers to infrastructure error and not
   * the task failure state.
   *
   * @param error A serialized {@link RemoteOnboardingTaskServiceException}
   * @since 1
   */
  oneway void onError(in IPersistableBundleStream error) = 2;
}
