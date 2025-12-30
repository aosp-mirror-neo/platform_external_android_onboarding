package com.android.onboarding.flags

import java.time.Duration

/** Interface for getting enabled state of different onboarding feature flags. */
interface OnboardingFlagsProvider {
  @Deprecated(message = "Replaced with a property", replaceWith = ReplaceWith("isContractEnabled"))
  fun isOnboardingContractEnabled(): Boolean = isContractEnabled

  /**
   * Returns `true` if the onboarding contract architecture is being used across the onboarding
   * flow.
   */
  val isContractEnabled: Boolean

  @Deprecated(
    message = "Replaced with a property",
    replaceWith = ReplaceWith("isNodeLoggingEnabled"),
  )
  fun isOnboardingNodeLoggingEnabled(): Boolean = isNodeLoggingEnabled

  /** Returns `true` if onboarding node logs should be uploaded remotely. */
  val isNodeLoggingEnabled: Boolean


  /** Returns `true` if onboarding UI logging should be uploaded remotely. */
  val isUiLoggingEnabled: Boolean
  /** Returns `true` if dimensions are being attached to logs. */
  val isDimensionLoggingEnabled: Boolean
  /**
   * Indicates that all onboarding components should activate the flagged changes regardless of
   * other flag values.
   */
  val isDebug: Boolean

  /** Returns true if node transitions should be visualised in logcat. */
  val shouldVisualiseNodeTransitionsInLogcat: Boolean

  val isNodeInvocationGilEnabled: Boolean

  val isProfileTypeInCommonEventMetadataEnabled: Boolean

  /** Returns `true` if the stable environment is enabled. */
  val isStableEnvironmentEnabled: Boolean

  /** Returns the amount of time after which the stable environment will be removed. */
  val stableEnvironmentAutoRemovalDelay: Duration

  /** Returns the initial delay when trying to set/unset a stable environment. */
  val stableEnvironmentRetryInitialDelay: Duration

  /** Returns the multiplier of previous delay in trying to set/unset a stable environment. */
  val stableEnvironmentRetryMultiplier: Double

  /** Returns the number of retries when trying to set/unset a stable environment. */
  val stableEnvironmentNumOfRetries: Int

  val isActivityMetadataLoggingEnabled: Boolean
}
