package com.google.android.wizardmanager

import android.os.Bundle
import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.getFromByteArray
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import com.android.onboarding.utils.putAsByteArray


/**
 * Contains contextual information used by Wizard Manager and by screens shown during Onboarding.
 *
 * This data is expected to be passed forward through the entire onboarding flow as part of
 * [WizardManagerExtras].
 *
 * Note that there is a [Bundle] representation and a [PersistableBundle] representation. These are
 * not identical and you should not convert between them without going via the [WizardBundle] class.
 * That is, `WizardBundle.fromPersistableBundle(persistableBundle).toBundle()` is a valid conversion
 * but `Bundle(persistableBundle)` is not.
 */
data class WizardBundle(val stack: WizardStack, val action: WizardAction) :
  Persistable, StableEquality {

  /** Writes the contents of this [WizardBundle] to the given [Bundle]. */
  fun writeToBundle(bundle: Bundle) {
    bundle.putAsByteArray(KEY_ACTION, action)
    bundle.putAsByteArray(KEY_STACK, stack)
  }

  /** Creates a new [Bundle] containing the contents of this [WizardBundle]. */
  fun toBundle(): Bundle = Bundle().also { writeToBundle(it) }

  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().also {
      it.putPersistable(KEY_ACTION, action)
      it.putPersistable(KEY_STACK, stack)
    }

  companion object : PersistableDeserializer<WizardBundle> {
    /**
     * Reads the data from a [Bundle].
     *
     * If any expected keys are missing, an error will be thrown.
     */
    fun fromBundle(bundle: Bundle): WizardBundle =
      WizardBundle(
        stack =
          bundle.getFromByteArray(KEY_STACK, WizardStack::class.java)
            ?: error("Missing WizardStack"),
        action =
          bundle.getFromByteArray(KEY_ACTION, WizardAction::class.java)
            ?: error("Missing WizardAction"),
      )

    /**
     * Reads the data from a [PersistableBundle].
     *
     * If any expected keys are missing, an error will be thrown.
     */
    override fun fromPersistableBundle(persistableBundle: PersistableBundle): WizardBundle =
      WizardBundle(
        stack =
          persistableBundle.getPersistable(KEY_STACK, Deserializers.WizardStack)
            ?: error("Missing WizardStack"),
        action =
          persistableBundle.getPersistable(KEY_ACTION, Deserializers.WizardAction)
            ?: error("Missing WizardAction"),
      )

    /** Parcelable instance of the WizardAction associated with an intent. */
    @JvmField val KEY_ACTION = "action"

    /** Parcelable instance of the WizardStack associated with an intent. */
    @JvmField val KEY_STACK = "stack"
  }
}

