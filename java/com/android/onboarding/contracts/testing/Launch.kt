package com.android.onboarding.contracts.testing

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.onboarding.contracts.OnboardingActivityApiContract

/**
 * Equivalent of [androidx.test.core.app.launchActivity] when working with contracts.
 *
 * @param intentExtras Extras to be copied into the intent that launches the activity.
 */
inline fun <reified ActivityT : Activity, I> launchContract(
  contract: OnboardingActivityApiContract<I, *>,
  input: I,
  intentExtras: Intent? = null,
): ActivityScenario<ActivityT> {
  val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), input)
  intentExtras?.let(intent::putExtras)
  return ActivityScenario.launch<ActivityT>(intent)
}

/**
 * Equivalent of [androidx.test.core.app.launchActivityForResult] when working with contracts.
 *
 * @param intentExtras Extras to be copied into the intent that launches the activity.
 */
inline fun <reified ActivityT : Activity, I> launchContractForResult(
  contract: OnboardingActivityApiContract<I, *>,
  input: I,
  intentExtras: Intent? = null,
): ActivityScenario<ActivityT> {
  val intent = contract.createIntent(ApplicationProvider.getApplicationContext(), input)
  intentExtras?.let(intent::putExtras)
  return ActivityScenario.launchActivityForResult<ActivityT>(intent)
}
