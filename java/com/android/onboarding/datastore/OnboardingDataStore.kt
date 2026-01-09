package com.android.onboarding.datastore

import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableSerializer
import com.google.common.util.concurrent.ListenableFuture

/**
 * A class for onboarding components to store objects in disk for the current SUW session.
 *
 * Blocking methods are offered to make it easier to migrate from synchronous I/O classes, like
 * [android.content.SharedPreferences] and [android.provider.Settings.Global]. These should be used
 * with caution, as blocking threads can lead to performance issues or deadlocks.
 */
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

  /** Blocking version of [write]. */
  fun <T> writeBlocking(key: String, value: T, serializer: PersistableSerializer<T>)

  /**
   * Blocking version of [write].
   *
   * This uses [Persistable.toPersistableBundle] to serialize [value].
   */
  fun <T : Persistable> writeBlocking(key: String, value: T)

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

  /** Blocking version of [read]. */
  fun <T> readBlocking(key: String, deserializer: PersistableDeserializer<T>): T?
}
