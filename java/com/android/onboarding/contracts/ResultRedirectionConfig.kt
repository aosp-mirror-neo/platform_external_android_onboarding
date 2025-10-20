package com.android.onboarding.contracts

import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.control.chained.ChainedContract
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableBundle
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable

/**
 * Configuration to be passed into an activity using an [OnboardingActivityApiContract] to control
 * how the result of that activity is handled.
 */
@InternalOnboardingApi
data class ResultRedirectionConfig(val nextActivity: ChainedContract.ActivityToActivity?) :
  Persistable, StableEquality {

  companion object : PersistableDeserializer<ResultRedirectionConfig> {

    /** The key used in intent extras to store a [ResultRedirectionConfig]. */
    const val EXTRA_KEY = "result_redirection_config"

    private const val KEY_NEXT_ACTIVITY = "next_activity"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      ResultRedirectionConfig(
        persistableBundle.getPersistable(KEY_NEXT_ACTIVITY, ChainedContract.ActivityToActivity)
      )
  }

  override fun toPersistableBundle() = PersistableBundle {
    putPersistable(KEY_NEXT_ACTIVITY, nextActivity)
  }
}
