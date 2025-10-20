package com.android.onboarding.contracts.annotations

import androidx.annotation.Keep

@Keep
@RequiresOptIn(
  message = "Marked entity is meant for internal use by the onboarding framework",
  level = RequiresOptIn.Level.ERROR,
)
annotation class InternalOnboardingApi
