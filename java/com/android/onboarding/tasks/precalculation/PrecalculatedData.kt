package com.android.onboarding.tasks.precalculation

import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer

/**
 * The result of executing a [PrecalculationTask].
 *
 * The data contained is only for use by the component which produced it. Other components should
 * only pass this along as a [PrecalculatedData] and should not read the data.
 */
data class PrecalculatedData private constructor(private val data: PersistableBundle) :
  Persistable {
  constructor(persistable: Persistable) : this(persistable.toPersistableBundle())

  /**
   * Extract this data into the type which produced it.
   *
   * This should match the type of the task which produced this data. It should only be read by the
   * component which owns the task which produced the data. There should be no dependencies built on
   * the contained data format.
   */
  fun <D : Persistable> extract(deserializer: PersistableDeserializer<D>) =
    deserializer.fromPersistableBundle(data)

  @InternalOnboardingApi override fun toPersistableBundle() = data

  companion object : PersistableDeserializer<PrecalculatedData> {
    @InternalOnboardingApi
    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      PrecalculatedData(persistableBundle)
  }
}
