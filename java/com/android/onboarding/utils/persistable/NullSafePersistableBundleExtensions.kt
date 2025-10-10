package com.android.onboarding.utils.persistable

import android.os.BaseBundle
import android.os.PersistableBundle
import com.android.onboarding.compatibility.compatGetBoolean

/**
 * Utility extension methods to retrieve elements from a bundle while explicitly stating whether to
 * throw or return a nullable if a key isn't found.
 */
object NullSafePersistableBundleExtensions {
  /**
   * Returns the value associated with the given key, or `null` if no [Long] value exists for the
   * given key.
   */
  fun BaseBundle.getLongOrNull(key: String?): Long? = getOrNull(key, ::getLong)

  /**
   * Returns the value associated with the given key, or throws an [NoSuchElementException] if no
   * [Int] value exists for the given key.
   */
  fun BaseBundle.getIntOrThrow(key: String?): Int = getOrThrow(key, ::getInt)

  /**
   * Returns the value associated with the given key, or throws an [NoSuchElementException] if no
   * [IntArray] value exists for the given key.
   */
  fun BaseBundle.getIntArrayOrThrow(key: String?): IntArray = getOrThrow(key, ::getIntArray)

  /**
   * Returns the value associated with the given key, or throws an [NoSuchElementException] if no
   * [Array<String>] value exists for the given key.
   */
  fun BaseBundle.getStringArrayOrThrow(key: String?): Array<String> =
    getOrThrow(key, ::getStringArray)

  /**
   * Returns the value associated with the given key, or throws an [NoSuchElementException] if no
   * [Long] value exists for the given key.
   */
  fun BaseBundle.getLongOrThrow(key: String?): Long = getOrThrow(key, ::getLong)

  /**
   * Returns the value associated with the given key, or throws an [NoSuchElementException] if no
   * [Boolean] value exists for the given key.
   */
  fun BaseBundle.getBooleanOrThrow(key: String?): Boolean = getOrThrow(key, ::compatGetBoolean)

  /**
   * Returns the value associated with the given key, or throws an [NoSuchElementException] if no
   * [Persistable] value exists for the given key.
   */
  inline fun <reified P : Persistable> PersistableBundle.getPersistableOrThrow(
    key: String?,
    deserializer: PersistableDeserializer<P>,
  ): P = getOrThrow(key) { getPersistable(key, deserializer)}

  /**
   * Returns the value associated with the given key, or throws an [NoSuchElementException] if no
   * [PersistableBundle] value exists for the given key.
   */
  fun PersistableBundle.getPersistableBundleOrThrow(key: String?): PersistableBundle =
    getOrThrow(key, ::getPersistableBundle)

  /**
   * Returns the value associated with the given key, or throws an [NoSuchElementException] if no
   * [String] value exists for the given key.
   */
  fun BaseBundle.getStringOrThrow(key: String?): String = getOrThrow(key, { k -> getString(k) })

  @PublishedApi
  internal inline fun <reified T : Any> BaseBundle.getOrNull(
    key: String?,
    getter: (String?) -> T?,
  ): T? = if (containsKey(key)) getter(key) else null

  @PublishedApi
  internal inline fun <reified T : Any> BaseBundle.getOrThrow(
    key: String?,
    getter: (String?) -> T?,
  ): T =
    getOrNull(key, getter)
      ?: throw NoSuchElementException("Key $key: ${T::class.java.name} is missing in the ${this::class.java.name}.")
}
