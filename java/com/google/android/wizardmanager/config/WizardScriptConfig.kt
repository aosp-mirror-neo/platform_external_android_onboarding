package com.google.android.wizardmanager.config

import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.Keep
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import kotlinx.parcelize.Parcelize

/** Configuration for a WizardScript and its children. */
@Parcelize
@Keep
data class WizardScriptConfig(val waitingScreenConfig: WaitingScreenConfig? = null) :
  Parcelable, Persistable, StableEquality {
  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().apply { putPersistable(KEY_WAITING_SCREEN_CONFIG, waitingScreenConfig) }

  companion object : PersistableDeserializer<WizardScriptConfig> {
    const val KEY_WAITING_SCREEN_CONFIG = "waitingScreenConfig"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): WizardScriptConfig =
      WizardScriptConfig(
        waitingScreenConfig =
          persistableBundle.getPersistable(KEY_WAITING_SCREEN_CONFIG, WaitingScreenConfig)
      )

    /**
     * Merge this config with a parent config.
     *
     * Returns the merged config.
     */
    fun merge(
      childConfig: WizardScriptConfig?,
      parentConfig: WizardScriptConfig?,
    ): WizardScriptConfig? {
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
      return WizardScriptConfig(mergedWaitingScreenConfig)
    }
  }
}
