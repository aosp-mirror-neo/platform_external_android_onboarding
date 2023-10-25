package com.android.onboarding.flags

import android.os.SystemProperties

/** A default implementation of [OnboardingFlagsProvider] using system property. */
class DefaultOnboardingFlagsProvider : OnboardingFlagsProvider {

  override fun isOnboardingContractEnabled(): Boolean {
    return SystemProperties.getBoolean(
      SYSTEM_PROPERTY_NAMESPACE + FEATURE_ONBOARDING_CONTRACT_ENABLED,
      false
    )
  }

  override fun isOnboardingNodeLoggingEnabled(): Boolean {
    return SystemProperties.getBoolean(
      SYSTEM_PROPERTY_NAMESPACE + FEATURE_ONBOARDING_NODE_LOGGING_ENABLED,
      false
    )
  }

  companion object {
    // The key length has to be less that 31 characters (<18 if excluding namespace)
    public const val SYSTEM_PROPERTY_NAMESPACE = "aoj.feature."
    private const val FEATURE_ONBOARDING_CONTRACT_ENABLED = "contract"
    private const val FEATURE_ONBOARDING_NODE_LOGGING_ENABLED = "node_logging"
  }
}
