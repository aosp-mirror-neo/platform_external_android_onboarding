package com.android.onboarding.flags

import android.os.Build
import java.time.Duration

/** A default implementation of [OnboardingFlagsProvider] using system property. */
@Deprecated(
  "Should not be used anymore as all flags managed by default implementation are now static." +
    "Prefer using OnboardingChanges instead."
)
class DefaultOnboardingFlagsProvider : OnboardingFlagsProvider {
  private val isDebugOr35: Boolean =
    Build.TYPE == "userdebug" || Build.TYPE == "eng" || Build.VERSION.SDK_INT >= 35
  override val isDebug: Boolean = false
  override val isContractEnabled: Boolean = isDebugOr35
  override val isNodeLoggingEnabled: Boolean = isDebugOr35
  override val shouldVisualiseNodeTransitionsInLogcat: Boolean = isDebugOr35
  override val isStableEnvironmentEnabled: Boolean = isDebugOr35
  override val stableEnvironmentAutoRemovalDelay: Duration = Duration.ofSeconds(300)
  override val stableEnvironmentRetryInitialDelay: Duration = Duration.ofSeconds(3)
  override val stableEnvironmentRetryMultiplier: Double = 1.5
  override val stableEnvironmentNumOfRetries: Int = 3
  // --- GIL ---
  override val isActivityMetadataLoggingEnabled: Boolean = false
  override val isUiLoggingEnabled: Boolean = true
  override val isDimensionLoggingEnabled: Boolean = false
  override val isNodeInvocationGilEnabled: Boolean = false
  override val isProfileTypeInCommonEventMetadataEnabled: Boolean = false
}
