package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality

/** [Persistable] wrapper for a [Long]. */
@JvmInline
value class PersistableLong(val value: Long) : Persistable, StableEquality {
  override fun toPersistableBundle() = toPersistableBundle(value)

  companion object : PersistableDeserializer<PersistableLong>, PersistableSerializer<Long> {
    private const val KEY = "value"

    @Suppress("DEPRECATION") // We need raw [get] calls.
    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      if (persistableBundle.get(KEY) is Long) PersistableLong(persistableBundle.getLong(KEY))
      else error("Invalid value for 'value' ('${persistableBundle.get(KEY)}')")

    override fun toPersistableBundle(value: Long) = PersistableBundle { putLong(KEY, value) }
  }
}
