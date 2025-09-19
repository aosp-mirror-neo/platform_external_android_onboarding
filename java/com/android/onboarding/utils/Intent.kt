package com.android.onboarding.utils

import android.content.Intent
import com.google.errorprone.annotations.CanIgnoreReturnValue

/**
 * Removes extras from the intent which have been set by Onboarding infrastructure.
 *
 * This should be used when re-using an intent - or using one intent as the basis for creating
 * another - to ensure that we do not accidentally reuse identifiers.
 */
@CanIgnoreReturnValue
@JvmOverloads
fun Intent.removeOnboardingExtras(exceptions: List<String> = listOf()): Intent {
  extras
    ?.keySet()
    ?.filter { it.startsWith("com.android.onboarding") && !exceptions.contains(it) }
    ?.forEach { removeExtra(it) }

  return this
}
