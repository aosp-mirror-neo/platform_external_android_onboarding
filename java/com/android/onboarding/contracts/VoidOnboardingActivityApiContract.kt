package com.android.onboarding.contracts

import androidx.activity.result.ActivityResult

/** Equivalent to [OnboardingActivityApiContract] for contracts which do not return a result. */
abstract class VoidOnboardingActivityApiContract<I> : OnboardingActivityApiContract<I, Unit>() {
  final override fun performSetResult(result: Unit): ActivityResult {
    // Does nothing - no result
    return ActivityResult(resultCode = 0, data = null)
  }

  final override fun performParseResult(result: ActivityResult) {
    // Does nothing - no result
  }
}
