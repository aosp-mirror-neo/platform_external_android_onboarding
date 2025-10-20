package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.contentDeepEquals
import com.android.onboarding.utils.generateHash
import com.android.onboarding.utils.toStringNoContent

/** A wrapper around [PersistableBundle] to make it fit the [Persistable] interface. */
class PersistablePersistableBundle(val persistableBundle: PersistableBundle) :
  Persistable, StableEquality {

  override fun equals(other: Any?) =
    other is PersistablePersistableBundle &&
      persistableBundle contentDeepEquals other.persistableBundle

  override fun hashCode() = persistableBundle.generateHash()

  override fun toString() = "PersistablePersistableBundle(${persistableBundle.toStringNoContent()})"

  /** Returns [persistableBundle] directly. */
  override fun toPersistableBundle(): PersistableBundle = toPersistableBundle(persistableBundle)

  companion object :
    PersistableDeserializer<PersistablePersistableBundle>,
    PersistableSerializer<PersistableBundle> {
    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      PersistablePersistableBundle(rawFromPersistableBundle(persistableBundle))

    /** Extracts raw [PersistableBundle] value written by [toPersistableBundle]. */
    fun rawFromPersistableBundle(persistableBundle: PersistableBundle) = persistableBundle

    override fun toPersistableBundle(value: PersistableBundle) = value
  }
}

/** Create a [PersistablePersistableBundle] for the [PersistableBundle]. */
fun PersistableBundle.toPersistable() = PersistablePersistableBundle(this)
