package com.android.onboarding.contracts

import android.content.Intent

/** Equivalent to [OnboardingActivityApiContract] for contracts which do not take arguments. */
abstract class ArgumentFreeOnboardingActivityApiContract<O> :
  OnboardingActivityApiContract<Unit, O>() {
  final override fun performExtractArgument(intent: Intent) {
    // Does nothing - no argument


    // how does this
    // affect metrics and logging?
  }
}
