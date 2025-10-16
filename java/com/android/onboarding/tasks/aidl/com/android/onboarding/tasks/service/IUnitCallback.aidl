package com.android.onboarding.tasks.service;

import com.android.onboarding.tasks.service.IPersistableBundleStream;

/**
 * A callback to be invoked when the execution completes.
 */
interface IUnitCallback {
  /**
   * Version of this implementation to know which methods are available.
   *
   * @since 1
   */
  int version() = 0;

  /**
   * Method invoked on successful execution.
   *
   * @since 1
   */
  oneway void onSuccess() = 1;

  /**
   * Method invoked on execution error.
   *
   * @param error A serialized {@link RemoteOnboardingTaskServiceException}
   * @since 1
   */
  oneway void onError(in IPersistableBundleStream error) = 2;
}
