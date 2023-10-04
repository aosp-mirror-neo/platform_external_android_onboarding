package com.android.onboarding.flags

/** Interface for getting enabled state of different onboarding feature flags. */
interface OnboardingFlagsProvider {

  /**
   * Returns [true] if the onboarding contract architecture is being used across the onboarding
   * flow.
   */
  fun isOnboardingContractEnabled(): Boolean

  /** Returns [true] if onboarding node logs should be uploaded remotely. */
  fun isOnboardingNodeLoggingEnabled(): Boolean
}
