package com.android.onboarding.config

import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.Keep
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import kotlinx.parcelize.Parcelize

/**
 * A config that controls which waiting screen to show when running tasks.
 *
 * @param contract A content provider contract for a waiting screen. For example:
 *   "contract:com.google.android.libraries.onboarding.contracts.setupwizard.waiting.OnboardingWaitingScreenContract"
 *   for the generic waiting screen.
 */
@Parcelize
@Keep
data class WaitingScreenConfig(val contract: String? = null) :
  Parcelable, Persistable, StableEquality {
  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().apply { putString(KEY_CONTRACT, contract) }

  companion object : PersistableDeserializer<WaitingScreenConfig> {
    internal const val KEY_CONTRACT = "contract"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      WaitingScreenConfig(persistableBundle.getString(KEY_CONTRACT))

    /**
     * Merge this config with a parent config. Child config wins.
     *
     * Returns the merged config.
     */
    fun merge(
      childConfig: WaitingScreenConfig?,
      parentConfig: WaitingScreenConfig?,
    ): WaitingScreenConfig? {
      if (childConfig == WaitingScreenConfig()) {
        // Child clears config.
        return null
      }
      return childConfig ?: parentConfig
    }
  }
}
