package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map

/**
 * Equivalent to [set] that can be used with [Persistable] objects.
 *
 * This must be paired with the *Persistable get methods (e.g. [getPersistable])
 */
fun SavedStateHandle.setPersistable(key: String, persistable: Persistable) {
  set(key, persistable.toPersistableBundle())
}

/**
 * Equivalent to [get] that can be used with [Persistable] objects.
 *
 * This should only be used to get keys which were set with [setPersistable].
 */
fun <T> SavedStateHandle.getPersistable(key: String, deserializer: PersistableDeserializer<T>): T? =
  get<PersistableBundle>(key)?.let { bundle -> deserializer.fromPersistableBundle(bundle) }

/**
 * Equivalent to [getLiveDate] that can be used with [Persistable] objects.
 *
 * This should only be used to get keys which were set with [setPersistable].
 */
fun <T> SavedStateHandle.getPersistableLiveData(
  key: String,
  deserializer: PersistableDeserializer<T>,
) = getLiveData<PersistableBundle>(key).map(deserializer::fromPersistableBundle)
