package com.android.onboarding.tasks.service;

import com.android.onboarding.tasks.service.IPersistableBundleStream;

/**
 * An observer to be invoked when the execution state updates.
 */
interface IObserver {

  /**
   * Version of this implementation to know which methods are available.
   *
   * @since 1
   */
  int version() = 0;

  /**
   * Method invoked on each execution state change.
   *
   * @param state Updated serialised execution state.
   * @since 1
   */
  oneway void onChange(in IPersistableBundleStream state) = 1;

  /**
   * Method invoked on execution error.
   *
   * @param error A serialized {@link RemoteOnboardingTaskServiceException}
   * @since 1
   */
  oneway void onError(in IPersistableBundleStream error) = 2;

  /**
   * Method invoked on execution completion,
   * when no more observer updates will be published.
   *
   * @since 1
   */
  oneway void onClose() = 3;
}
