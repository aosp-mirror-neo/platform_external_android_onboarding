package com.google.android.wizardmanager.config

import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.Keep
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import kotlinx.parcelize.Parcelize

/** Configuration for a WizardScript and its children. */
@Parcelize
@Keep
data class WizardScriptConfig(val unused: Boolean? = null) :
  Parcelable, Persistable, StableEquality {
  override fun toPersistableBundle(): PersistableBundle = PersistableBundle()

  companion object : PersistableDeserializer<WizardScriptConfig> {
    override fun fromPersistableBundle(persistableBundle: PersistableBundle): WizardScriptConfig =
      WizardScriptConfig()
  }
}
