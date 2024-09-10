package com.android.onboarding.contracts.annotations

/**
 * An opt-in annotation intended to force explicit enrollment into usage of the discouraged APIs
 *
 * @property recommendation a message explaining which APIs should be used instead
 */
@RequiresOptIn(
  message =
    "Marked API is only meant as a temporary workaround for legacy consumers " +
      "that cannot immediately use the recommended onboarding APIs " +
      "and as such demands an explicit opt-in"
)
annotation class DiscouragedOnboardingApi(val recommendation: String)
