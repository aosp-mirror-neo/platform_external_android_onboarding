package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import com.android.onboarding.compatibility.compatGetBoolean
import com.android.onboarding.compatibility.compatPutBoolean
import com.android.onboarding.utils.StableEquality

/** [Persistable] wrapper for a [Boolean]. */
@JvmInline
value class PersistableBoolean(val value: Boolean) : Persistable, StableEquality {
  override fun toPersistableBundle() = toPersistableBundle(value)

  companion object : PersistableDeserializer<PersistableBoolean>, PersistableSerializer<Boolean> {
    private const val KEY = "value"

    override fun toPersistableBundle(value: Boolean): PersistableBundle = PersistableBundle {
      compatPutBoolean(KEY, value)
    }

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): PersistableBoolean =
      PersistableBoolean(getBoolean(persistableBundle))

    fun getBoolean(persistableBundle: PersistableBundle): Boolean =
      if (persistableBundle.containsKey(KEY)) {
        persistableBundle.compatGetBoolean(KEY)
      } else {
        error("$persistableBundle doesn't contain $KEY")
      }
  }
}
