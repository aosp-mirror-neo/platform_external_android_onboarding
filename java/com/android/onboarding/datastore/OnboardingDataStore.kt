package com.android.onboarding.datastore

import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableSerializer
import com.google.common.util.concurrent.ListenableFuture

/** A class for onboarding components to store objects in disk for the current SUW session. */
interface OnboardingDataStore {

  /**
   * Writes the [value] into the disk for the current SUW session.
   *
   * @see read
   */
  suspend fun <T> write(key: String, value: T, serializer: PersistableSerializer<T>)

  /**
   * Writes the [value] into the disk for the current SUW session.
   *
   * This uses [Persistable.toPersistableBundle] to serialize [value].
   *
   * @see read
   */
  suspend fun <T : Persistable> write(key: String, value: T)

  /** Non-suspend version of [write]. */
  fun <T> writeAsync(
    key: String,
    value: T,
    serializer: PersistableSerializer<T>,
  ): ListenableFuture<Unit>

  /**
   * Non-suspend version of [write].
   *
   * This uses [Persistable.toPersistableBundle] to serialize [value].
   */
  fun <T : Persistable> writeAsync(key: String, value: T): ListenableFuture<Unit>

  /**
   * Reads from the disk for the current SUW session.
   *
   * Returns `null` if no value is found.
   *
   * @see write
   */
  suspend fun <T> read(key: String, deserializer: PersistableDeserializer<T>): T?

  /** Non-suspend version of [read]. */
  fun <T> readAsync(key: String, deserializer: PersistableDeserializer<T>): ListenableFuture<T?>
}
