package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality

/** [Persistable] wrapper for an [Double]. */
@JvmInline
value class PersistableDouble(val value: Double) : Persistable, StableEquality {
  override fun toPersistableBundle() = toPersistableBundle(value)

  companion object : PersistableDeserializer<PersistableDouble>, PersistableSerializer<Double> {
    private const val KEY = "value"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      PersistableDouble(rawFromPersistableBundle(persistableBundle))

    /** Extracts raw [Int] value written by [toPersistableBundle]. */
    @Suppress("DEPRECATION") // We need raw [get] calls.
    fun rawFromPersistableBundle(persistableBundle: PersistableBundle) =
      if (persistableBundle.get(KEY) is Double) persistableBundle.getDouble(KEY)
      else error("Invalid value for 'value' ('${persistableBundle.get(KEY)}')")

    override fun toPersistableBundle(value: Double) = PersistableBundle { putDouble(KEY, value) }
  }
}
