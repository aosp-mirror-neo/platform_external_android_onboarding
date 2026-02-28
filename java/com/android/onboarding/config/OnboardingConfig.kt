package com.android.onboarding.config

import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.Keep
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import kotlinx.parcelize.Parcelize

/** A container for various onboarding related configurations. */
@Parcelize
@Keep
data class OnboardingConfig(val waitingScreenConfig: WaitingScreenConfig? = null) :
  Parcelable, Persistable, StableEquality {
  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().apply { putPersistable(KEY_WAITING_SCREEN_CONFIG, waitingScreenConfig) }

  companion object : PersistableDeserializer<OnboardingConfig> {
    const val KEY_WAITING_SCREEN_CONFIG = "waitingScreenConfig"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): OnboardingConfig =
      OnboardingConfig(
        waitingScreenConfig =
          persistableBundle.getPersistable(KEY_WAITING_SCREEN_CONFIG, WaitingScreenConfig)
      )

    /**
     * Merge this config with a parent config.
     *
     * Returns the merged config.
     */
    fun merge(childConfig: OnboardingConfig?, parentConfig: OnboardingConfig?): OnboardingConfig? {
      if (childConfig == null && parentConfig == null) {
        return null
      }
      if (parentConfig == null) {
        // Nothing to override.
        return childConfig
      }
      if (childConfig == null) {
        // Override everything with the parent config.
        return parentConfig
      }
      val mergedWaitingScreenConfig =
        WaitingScreenConfig.merge(childConfig.waitingScreenConfig, parentConfig.waitingScreenConfig)
      return OnboardingConfig(mergedWaitingScreenConfig)
    }
  }
}
