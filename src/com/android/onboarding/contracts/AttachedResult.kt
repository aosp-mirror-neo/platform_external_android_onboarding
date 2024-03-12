package com.android.onboarding.contracts

/**
 * Represents the attached result after running [OnboardingActivityApiContract.attach].
 *
 * @property valid `true` if the intent is valid, `false` otherwise.
 */
data class AttachedResult(val valid: Boolean)
