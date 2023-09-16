package com.android.onboarding.flags.testing

import com.android.onboarding.flags.OnboardingFlagsProvider

/** A fake implementation of [OnboardingFlagsProvider]. */
class FakeOnboardingFlagsProvider(var isOnboardingContractEnabledFlag: Boolean = false) :
  OnboardingFlagsProvider {
  override fun isOnboardingContractEnabled() = isOnboardingContractEnabledFlag
}
