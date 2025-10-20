package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality

/** [Persistable] wrapper for an [Int]. */
@JvmInline
value class PersistableInt(val value: Int) : Persistable, StableEquality {
  override fun toPersistableBundle() = toPersistableBundle(value)

  companion object : PersistableDeserializer<PersistableInt>, PersistableSerializer<Int> {
    private const val KEY = "value"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      PersistableInt(rawFromPersistableBundle(persistableBundle))

    /** Extracts raw [Int] value written by [toPersistableBundle]. */
    @Suppress("DEPRECATION") // We need raw [get] calls.
    fun rawFromPersistableBundle(persistableBundle: PersistableBundle) =
      if (persistableBundle.get(KEY) is Int) persistableBundle.getInt(KEY)
      else error("Invalid value for 'value' ('${persistableBundle.get(KEY)}')")

    override fun toPersistableBundle(value: Int) = PersistableBundle { putInt(KEY, value) }
  }
}
