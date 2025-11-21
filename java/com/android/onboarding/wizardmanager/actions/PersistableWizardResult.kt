package com.android.onboarding.wizardmanager.actions

import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableIntent
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable

/** A result used for tasks which are compatible with WizardManager. */
data class PersistableWizardResult(val resultCode: Int, val intent: PersistableIntent? = null) :
  Persistable, StableEquality {

  override fun toString(): String {
    return "PWR{$resultCode}"
  }

  override fun toPersistableBundle() =
    PersistableBundle().apply {
      putInt(RESULT_CODE_KEY, resultCode)
      if (intent != null) {
        putPersistable(INTENT_KEY, intent)
      }
    }

  companion object : PersistableDeserializer<PersistableWizardResult> {
    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      PersistableWizardResult(
        resultCode = persistableBundle.getInt(RESULT_CODE_KEY),
        intent = persistableBundle.getPersistable(INTENT_KEY, PersistableIntent),
      )

    private const val RESULT_CODE_KEY = "result_code"
    private const val INTENT_KEY = "intent"
  }
}
