package com.android.onboarding.contracts.testing

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.android.onboarding.contracts.IntentSerializer
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.google.common.truth.Truth.assertThat
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/** Assert that a contract's arguments encode correctly. */
fun <I> assertArgumentEncodesCorrectly(contract: OnboardingActivityApiContract<I, *>, argument: I) {
  val context = ApplicationProvider.getApplicationContext<Context>()
  val intent = contract.createIntent(context, argument)
  val out = contract.extractArgument(intent)

  assertThat(out).isEqualTo(argument)
}

/**
 * Assert that a contract's result encodes correctly.
 *
 * <p>Due to an implementation detail, tests using this must be using RobolectricTestRunner
 */
fun <O> assertReturnValueEncodesCorrectly(contract: OnboardingActivityApiContract<*, O>, value: O) {
  val controller = Robolectric.buildActivity(Activity::class.java)
  /*
  * Cannot use [AutoCloseable::use] since [ActivityController]
  * does not implement [AutoCloseable] on AOSP
  */
  try {
    val activity = controller.get()
    contract.setResult(activity, value)
    val shadowActivity = shadowOf(activity)
    val result = contract.parseResult(shadowActivity.resultCode, shadowActivity.resultIntent)

    assertThat(result).isEqualTo(value)
  } finally {
    controller.pause()
    controller.stop()
    controller.destroy()
  }
}

/** Assert that an object can be correctly written to and parsed from an Intent. */
fun <I> assertIntentEncodesCorrectly(parser: IntentSerializer<I>, obj: I) {
  val intent = Intent()
  parser.write(intent, obj)
  val out = parser.read(intent)

  assertThat(out).isEqualTo(obj)
}
