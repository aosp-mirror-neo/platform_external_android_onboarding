package com.android.onboarding.flags

/** Interface for getting enabled state of different onboarding feature flags. */
interface OnboardingFlagsProvider {

  /**
   * True if onboarding contracts[setupwizard.feature.is_onboarding_contract_enabled] are being used
   * across the onboarding flow.
   */
  fun isOnboardingContractEnabled(): Boolean
}
