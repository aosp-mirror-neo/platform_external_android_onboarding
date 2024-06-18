package com.android.onboarding.process

/**
 * Interface representing the Onboarding Process responsible for keeping the calling process alive
 * to prevent it from being terminated by the system.
 */
interface OnboardingProcess {

  /**
   * API used to keep the calling process alive. Accepts a [reason] explaining why the calling
   * process should remain alive. Returns an [AutoCloseable], allowing for both lambda parameters
   * and termination using the close API.
   */
  fun keepAlive(reason: KeepAliveReason): AutoCloseable
}

/**
 * Enum representing reasons for keeping an onboarding process alive. These reasons are used to
 * determine when to utilize the Keep Alive API.
 */
enum class KeepAliveReason {
  TEST_ONLY, // For testing purposes
  OTHER, // Other unspecified reasons
}
