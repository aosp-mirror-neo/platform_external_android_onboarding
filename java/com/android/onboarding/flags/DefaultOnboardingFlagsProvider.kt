package com.android.onboarding.flags

import android.os.Build
import android.os.SystemProperties
import java.time.Duration

/** A default implementation of [OnboardingFlagsProvider] using system property. */
class DefaultOnboardingFlagsProvider : OnboardingFlagsProvider {
  private fun getSystemPropertyBoolean(key: String, default: Boolean): Boolean =
    if (Build.VERSION.SDK_INT >= 28) {
      SystemProperties.getBoolean(key, default)
    } else {
      false
    }


  override val isContractEnabled: Boolean
    get() = getSystemPropertyBoolean("$SYSTEM_PROPERTY_NAMESPACE.$FEATURE_CONTRACT_ENABLED", false)

  // Node logging is enabled by default on >= Android V devices
  override val isNodeLoggingEnabled: Boolean
    get() =
      getSystemPropertyBoolean(
        "$SYSTEM_PROPERTY_NAMESPACE.$FEATURE_NODE_LOGGING_ENABLED",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM,
      )

  override val isActivityMetadataLoggingEnabled: Boolean
    get() = false


  override val isUiLoggingEnabled: Boolean
    get() = true

  override val isDimensionLoggingEnabled: Boolean
    get() = false

  override val isDebug: Boolean
    get() = getSystemPropertyBoolean("$SYSTEM_PROPERTY_NAMESPACE.$FEATURE_DEBUG_ENABLED", false)

  override val shouldVisualiseNodeTransitionsInLogcat: Boolean
    get() =
      getSystemPropertyBoolean(
        "$SYSTEM_PROPERTY_NAMESPACE.$FEATURE_VISUALISE_TRANSITIONS",
        false,
      ) || isDebug

  override val isNodeInvocationGilEnabled: Boolean
    get() = false

  override val isProfileTypeInCommonEventMetadataEnabled: Boolean
    get() = false

  override val isStableEnvironmentEnabled: Boolean
    get() = false

  override val stableEnvironmentAutoRemovalDelay: Duration
    get() = Duration.ofSeconds(300)

  override val stableEnvironmentRetryInitialDelay: Duration
    get() = Duration.ofSeconds(3)

  override val stableEnvironmentRetryMultiplier: Double
    get() = 1.5

  override val stableEnvironmentNumOfRetries: Int
    get() = 3

  companion object {
    // The key length has to be less that 31 characters (<18 if excluding namespace)

    const val SYSTEM_PROPERTY_NAMESPACE = "aoj.feature"
    private const val FEATURE_CONTRACT_ENABLED = "contract"
    private const val FEATURE_NODE_LOGGING_ENABLED = "node_logging"
    private const val FEATURE_DEBUG_ENABLED = "debug"
    private const val FEATURE_VISUALISE_TRANSITIONS = "vis_transitions"
    private const val FEATURE_DIMENSION_LOGGING_ENABLED = "dimension_logging"
  }
}
