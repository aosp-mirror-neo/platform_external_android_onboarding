package com.android.onboarding.flags.testing

import com.android.onboarding.flags.OnboardingFlagsProvider
import java.time.Duration

/** A fake implementation of [OnboardingFlagsProvider]. */
class FakeOnboardingFlagsProvider(
  override var isContractEnabled: Boolean = false,
  override var isNodeLoggingEnabled: Boolean = false,

  override var isUiLoggingEnabled: Boolean = true,
  override var isDimensionLoggingEnabled: Boolean = false,
  override var isDebug: Boolean = false,
  override val shouldVisualiseNodeTransitionsInLogcat: Boolean = false,
  override var isNodeInvocationGilEnabled: Boolean = false,
  override var isProfileTypeInCommonEventMetadataEnabled: Boolean = false,
  override var isStableEnvironmentEnabled: Boolean = false,
  override var stableEnvironmentAutoRemovalDelay: Duration = Duration.ofSeconds(300),
  override var stableEnvironmentRetryInitialDelay: Duration = Duration.ofSeconds(3),
  override var stableEnvironmentRetryMultiplier: Double = 1.5,
  override var stableEnvironmentNumOfRetries: Int = 3,
) : OnboardingFlagsProvider {
  @Deprecated(message = "Replaced with overrides", replaceWith = ReplaceWith("isContractEnabled"))
  var isOnboardingContractEnabledFlag: Boolean by ::isContractEnabled

  @Deprecated(
    message = "Replaced with overrides",
    replaceWith = ReplaceWith("isNodeLoggingEnabled"),
  )
  var isOnboardingNodeLoggingEnabledFlag: Boolean by ::isNodeLoggingEnabled
}
