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
    public const val SYSTEM_PROPERTY_NAMESPACE = "aoj.architecture.feature."
    private const val FEATURE_ONBOARDING_CONTRACT_ENABLED = "is_onboarding_contract_enabled"
    private const val FEATURE_ONBOARDING_NODE_LOGGING_ENABLED = "is_onboarding_node_logging_enabled"
  }
}
