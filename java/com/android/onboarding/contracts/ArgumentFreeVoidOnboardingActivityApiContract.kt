package com.android.onboarding.contracts

import android.content.Context
import android.content.Intent

/** Equivalent to [VoidOnboardingActivityApiContract] for contracts which do not take arguments. */
abstract class ArgumentFreeVoidOnboardingActivityApiContract :
  VoidOnboardingActivityApiContract<Unit>() {
  final override fun performExtractArgument(intent: Intent) {
    // Does nothing - no argument
  }

  final override fun performCreateIntent(context: Context, arg: Unit) = performCreateIntent(context)

  abstract fun performCreateIntent(context: Context): Intent
}
