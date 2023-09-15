package com.android.onboarding.flags

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle

/** A default implementation of [OnboardingFlagsProvider]. */
class DefaultOnboardingFlagsProvider(val appContext: Context) : OnboardingFlagsProvider {

  /**
   * [OnboardingFlagsProvider] access a specific content provider in the setup wizard, and setup
   * wizard maintains the onboarding contract flag logic behind itself.
   */
  override fun isOnboardingContractEnabled(): Boolean {
    val requestFlag = arrayOf(FEATURE_ONBOARDING_CONTRACT_ENABLED)
    val requestBundle = Bundle().apply { putStringArray(EXTRA_KEY_FEATURES, requestFlag) }

    val result =
      appContext.contentResolver.call(
        getContentUri(),
        METHOD_GET_FEATURES,
        /* arg= */ null,
        /* extras= */ requestBundle
      )
    return result?.getBoolean(FEATURE_ONBOARDING_CONTRACT_ENABLED) ?: false
  }

  private fun getContentUri(): Uri =
    Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(SUW_AUTHORITY).build()

  companion object {
    const val SUW_AUTHORITY = "com.google.android.setupwizard.FEATURES"
    const val METHOD_GET_FEATURES = "features"
    const val EXTRA_KEY_FEATURES = "features"
    const val FEATURE_ONBOARDING_CONTRACT_ENABLED = "is_onboarding_contract_enabled"
  }
}
