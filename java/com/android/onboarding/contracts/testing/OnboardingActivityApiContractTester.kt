package com.android.onboarding.contracts.testing

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.android.onboarding.contracts.IntentSerializer
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.testing.recursiveReflectionEquals
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

const val TEST_NODE_ID: NodeId = -666

/** Assert that a contract's arguments encode correctly. */
fun <I> assertArgumentEncodesCorrectly(
  contract: OnboardingActivityApiContract<I, *>,
  argument: I,
  checker: (result: I, expected: I) -> Unit = { r, e ->
    assertThat(recursiveReflectionEquals(r, e)).isTrue()
  },
) {
  val context = ApplicationProvider.getApplicationContext<Context>()
  val intent = contract.createIntent(context, argument)
  val out = contract.extractArgument(intent)

  checker(out, argument)
}

/**
 * Assert that a contract's result encodes correctly.
 *
 * <p>Due to an implementation detail, tests using this must be using RobolectricTestRunner
 */
fun <O> assertReturnValueEncodesCorrectly(
  contract: OnboardingActivityApiContract<*, O>,
  value: O,
  checker: (result: O, expected: O) -> Unit = { r, e ->
    assertThat(recursiveReflectionEquals(r, e)).isTrue()
  },
) {
  val controller = Robolectric.buildActivity(Activity::class.java)
  /*
   * Cannot use [AutoCloseable::use] since [ActivityController]
   * does not implement [AutoCloseable] on AOSP
   */
  try {
    val activity = controller.get()
    contract.prepareResult(value).commitSync(activity)
    val shadowActivity = shadowOf(activity)
    val result = contract.parseResult(shadowActivity.resultCode, shadowActivity.resultIntent)

    checker(result, value)
  } finally {
    controller.pause()
    controller.stop()
    controller.destroy()
  }
}

/** Assert that an object can be correctly written to and parsed from an Intent. */
fun <I> assertIntentEncodesCorrectly(
  parser: IntentSerializer<I>,
  obj: I,
  checker: (result: I, expected: I) -> Unit = { r, e ->
    assertThat(recursiveReflectionEquals(r, e)).isTrue()
  },
) {
  val intent = Intent()
  parser.write(intent, obj)
  val out = parser.read(intent)

  checker(out, obj)
}

/**
 * Assert that a given [serializer] decodes an empty [Intent] without throwing an exception (fails
 * lazily on property access).
 */
fun <I : Any> assertEmptyIntentDecodingFailsLazily(serializer: IntentSerializer<I>) {
  val intent = Intent()
  val arg = serializer.read(intent)
  val failures =
    arg::class
      .memberProperties
      .map { @Suppress("UNCHECKED_CAST") (it as KProperty1<I, *>) }
      .map { runCatching { it.get(arg) } }
      .filter(Result<*>::isFailure)
  assertWithMessage("Accessing parsed properties throws lazy errors").that(failures).isNotEmpty()
}
