package com.android.onboarding.bedsteadonboarding

import android.annotation.SuppressLint
import com.android.bedstead.harrier.BedsteadJUnit4
import com.android.bedstead.harrier.DeviceState
import org.junit.ClassRule
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * A base class for onboarding tests to extend providing common onboarding and bedstead
 * configurations.
 */
@RunWith(OnboardingRunner::class)
abstract class OnboardingTest {
  companion object {
    @get:JvmStatic
    @get:ClassRule
    @get:Rule
    @SuppressLint("StaticFieldLeak")
    val deviceState: DeviceState = DeviceState()
  }

  @get:Rule val onboarding = OnboardingTestsRule()
}

/** An alias for junit test runner used by the onboarding. */
typealias OnboardingRunner = BedsteadJUnit4
