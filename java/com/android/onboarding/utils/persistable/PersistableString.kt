package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.PersistableString.Companion.toPersistableBundle

@JvmInline
value class PersistableString(val value: String) : Persistable, StableEquality {
  override fun toPersistableBundle() = toPersistableBundle(value)

  companion object : PersistableDeserializer<PersistableString>, PersistableSerializer<String> {
    const val KEY_VALUE = "value"

    override fun toPersistableBundle(value: String) = PersistableBundle {
      putString(KEY_VALUE, value)
    }

    /** Extracts raw [String] value written by [toPersistableBundle]. */
    fun rawFromPersistableBundle(persistableBundle: PersistableBundle) =
      requireNotNull(persistableBundle.getString(KEY_VALUE)) { "PersistableString has no value" }

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      PersistableString(rawFromPersistableBundle(persistableBundle))
  }
}
