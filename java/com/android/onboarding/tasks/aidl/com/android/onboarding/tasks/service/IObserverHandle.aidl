package com.android.onboarding.tasks.service;

/**
 * A handle of an observer, providing unsubscribe capability.
 */
interface IObserverHandle {

  /**
   * Version of this implementation to know which methods are available.
   *
   * @since 1
   */
  int version() = 0;

  /**
   * Closes the observer associated with this handle.
   *
   * @since 1
   */
  oneway void close() = 1;
}
