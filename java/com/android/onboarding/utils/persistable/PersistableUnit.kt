package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality
import kotlin.Unit as KUnit

/** Object which can be used for APIs which require [Persistable] when no value is permitted. */
data object PersistableUnit :
  Persistable,
  PersistableDeserializer<PersistableUnit>,
  PersistableSerializer<KUnit> by Unit,
  StableEquality {
  inline val value
    get() = KUnit

  override fun toPersistableBundle(): PersistableBundle = PersistableBundle()

  override fun fromPersistableBundle(persistableBundle: PersistableBundle) = PersistableUnit

  /** [PersistableDelegate] to work with raw [Unit][kotlin.Unit] values. */
  object Unit : PersistableDelegate<KUnit> {
    override fun toPersistableBundle(value: KUnit): PersistableBundle = toPersistableBundle()

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) = KUnit
  }
}
