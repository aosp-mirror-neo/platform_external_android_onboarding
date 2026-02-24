package com.android.onboarding.utils.persistable

import android.accounts.Account
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.PersistableBundle
import android.util.Size
import androidx.annotation.VisibleForTesting
import com.android.onboarding.compatibility.compatPutBoolean
import com.android.onboarding.compatibility.compatPutBooleanArray
import com.android.onboarding.utils.logging.OnboardingLog as Log
import java.util.Locale

internal const val LOG_TAG = "BundleExt"

/**
 * Returns a new [PersistableBundle] containing all persistable values.
 *
 * Non-persistable values will have their types converted or will be dropped.
 * * [Byte]s will be stored as [Int]s (and [Byte] arrays as [Int] arrays)
 * * [Short]s will be stored as [Short]s (and [Short] arrays as [Int] arrays)
 * * [Char]s will be stored as [String]s (and [Char] arrays as [String] arrays)
 * * [Float]s will be stored as [Double]s (and [Float] arrays as [Double] arrays)
 * * [Size]s will be stored as [String]s
 * * [CharSequence]s will be stored as [String]s (and [CharSequence] arrays or [ArrayList]s as
 *   [String] arrays)
 * * [ComponentName] will be stored as a flattened [String]
 * * [Account]s will be stored as [PersistableBundle]s
 * * [Locale] will be stored as a string
 * * [Uri] will be stored as a string
 * * [Intent] will be stored as a [PersistableBundle]
 * * [Bundle]s will themselves be run through [.toPersistableBundle] with the same [onError] and be
 *   stored as [PersistableBundle]s (and [Bundle] arrays will be stored as [PersistableBundle] of
 *   elements each converted).
 *
 * The resulting [PersistableBundle] will also contain a Bundle under the [KEY_CONVERSION] key which
 * contains metadata about the conversion. That contains a [KEY_ORIGINAL_TYPES] key which represents
 * a Bundle mapping keys to their types in the original [Bundle].
 *
 * @param onError if passed will be called whenever a key cannot be converted. If null then nothing
 *   will happen.
 */
fun Bundle.toPersistableBundle(
  onError: ((key: String, error: String) -> Unit)? = null
): PersistableBundle {
  // We parcel then unparcel the bundle to drop the specific types
  parcelAndUnparcel().apply {
    val persistableBundle = PersistableBundle()
    val converted = PersistableBundle()

    for (key in keySet()) {
      var value: Any? = null
      try {
        @Suppress("DEPRECATION")
        value = get(key)
      } catch (e: Exception) {
        if (onError == null) {
          Log.d(LOG_TAG, "Dropping object with key $key due to error: $e")
        } else {
          onError(key, "Dropping object with key $key due to error: $e")
        }
      }
      if (value == null) {
        continue
      }
      persistableBundle.putValue(key, value, converted, onError)
    }

    val conversion =
      PersistableBundle().apply { putPersistableBundle(KEY_ORIGINAL_TYPES, converted) }
    persistableBundle.putPersistableBundle(KEY_CONVERSION, conversion)

    return persistableBundle
  }
}

private fun PersistableBundle.putValue(
  key: String,
  value: Any,
  converted: PersistableBundle,
  onError: ((key: String, error: String) -> Unit)?,
) {
  val topLevelClassName = value::class.qualifiedName
  converted.putString(key, topLevelClassName)
  when (value) {
    // Supported types
    is Int -> putInt(key, value)
    is IntArray -> putIntArray(key, value)
    is Long -> putLong(key, value)
    is LongArray -> putLongArray(key, value)
    is Double -> putDouble(key, value)
    is DoubleArray -> putDoubleArray(key, value)
    is Boolean -> compatPutBoolean(key, value)
    is BooleanArray -> compatPutBooleanArray(key, value)
    is String -> putString(key, value)
    is PersistableBundle -> putPersistableBundle(key, value)
    is Persistable -> putPersistable(key, value)

    // Unsupported types we can convert
    is Bundle -> putPersistableBundle(key, value.toPersistableBundle(onError))
    is Byte -> putInt(key, value.toInt())
    is ByteArray -> putIntArray(key, value.map { it.toInt() }.toIntArray())
    is Short -> putInt(key, value.toInt())
    is ShortArray -> putIntArray(key, value.map { it.toInt() }.toIntArray())
    is Char -> putString(key, value.toString())
    is CharArray -> putStringArray(key, value.map { it.toString() }.toTypedArray())
    is CharSequence -> putString(key, value.toString())
    is Float -> putDouble(key, value.toDouble())
    is FloatArray -> putDoubleArray(key, value.map { it.toDouble() }.toDoubleArray())
    is Size -> putString(key, value.toString())
    is ComponentName -> putString(key, value.flattenToString())
    is Intent -> putPersistable(key, PersistableIntent(value))

    is Account -> putPersistableBundle(key, toPersistableBundle(value))
    is Locale -> putString(key, value.toLanguageTag())
    is Uri -> {
      // Specify Uri type to avoid subclass issues e.g. StringUri
      converted.putString(key, Uri::class.qualifiedName)
      putString(key, value.toString())
    }
    is ArrayList<*> -> putListValue(key, value, converted, onError, topLevelClassName)
    is Array<*> -> putListValue(key, value.asList(), converted, onError, topLevelClassName)
    else ->
      if (onError == null) {
        Log.d(
          LOG_TAG,
          "Dropping object with key $key due to unknown or unpersistable type ${value.javaClass}",
        )
      } else {
        onError(
          key,
          "Dropping object with key $key due to unknown or unpersistable type ${value.javaClass}",
        )
      }
  }
}

private fun PersistableBundle.putListValue(
  key: String,
  value: Collection<Any?>,
  converted: PersistableBundle,
  onError: ((key: String, error: String) -> Unit)?,
  topLevelClassName: String?,
) {
  if (value.size == 0) {
    Log.d(
      LOG_TAG,
      "Dropping empty $topLevelClassName with key $key because we can't determine the type",
    )
  } else {
    val arrayValue = value.firstNotNullOfOrNull { it }

    if (arrayValue == null) {
      // We store an array of nulls - type shouldn't matter - and we can't know the
      // intended type
      putStringArray(key, arrayOfNulls(value.size))
      return
    }

    converted.putString(key, "$topLevelClassName<${arrayValue::class.qualifiedName}>")
    when (arrayValue) {
      is Int -> putIntArray(key, value.map { it as Int }.toIntArray())
      is String -> putStringArray(key, value.map { it?.toString() }.toTypedArray())
      is CharSequence -> putStringArray(key, value.map { it?.toString() }.toTypedArray())
      is Bundle ->
        putPersistableList(
          key,
          value.map { (it as? Bundle)?.toPersistableBundle(onError) },
          PersistablePersistableBundle,
        )
      else ->
        if (onError == null) {
          Log.d(
            LOG_TAG,
            "Dropping $topLevelClassName with key $key due to unknown or unpersistable type ${arrayValue.javaClass}",
          )
        } else {
          onError(
            key,
            "$topLevelClassName with key $key contains unknown or unpersistable type ${arrayValue.javaClass}",
          )
        }
    }
  }
}

/** Write [value] to [Bundle] via this [NullablePersistableSerializer]. */
fun <T> NullablePersistableSerializer<T>.toBundle(value: T): Bundle? =
  toPersistableBundle(value)?.toBundle()

/** Write [value] to [Bundle] via this [PersistableSerializer]. */
fun <T> PersistableSerializer<T>.toBundle(value: T): Bundle = toPersistableBundle(value).toBundle()

/** Read [T] from [bundle] via this [PersistableDeserializer]. */
fun <T> PersistableDeserializer<T>.fromBundle(bundle: Bundle) =
  bundle.toPersistableBundle().let(::fromPersistableBundle)

/** Writes a [Persistable] to the [Bundle]. */
fun Bundle.putPersistable(key: String?, value: Persistable?) =
  putPersistable(key, value, Persistable)

/** Writes a [Persistable] to the [Bundle]. */
fun <T> Bundle.putPersistable(
  key: String?,
  value: T?,
  serializer: NullablePersistableSerializer<T>,
) = putBundle(key, value?.let(serializer::toPersistableBundle)?.toBundle())

/** Gets the [Persistable] with the given key. */
fun <T> Bundle.getPersistable(key: String, deserializer: PersistableDeserializer<T>): T? =
  getBundle(key)?.toPersistableBundle()?.let(deserializer::fromPersistableBundle)

/** Return a new Bundle which is the result of parceling and unparceling this Bundle. */
fun Bundle.parcelAndUnparcel(): Bundle {
  val p = Parcel.obtain()
  try {
    p.writeBundle(this)
    p.setDataPosition(0)
    return p.readBundle(classLoader)
      ?: error("Should never happen... we just wrote a bundle and can't read it back")
  } finally {
    p.recycle()
  }
}

/**
 * Remove all keys in the given set from the [Bundle] and from all [Bundle]s recursively inside the
 * top-level [Bundle].
 */
fun Bundle.recursivelyRemoveKeys(matching: Set<String>) {
  for (k in matching) {
    remove(k)
  }
  for (key in keySet()) {
    getBundle(key)?.recursivelyRemoveKeys(matching)
  }
}

/**
 * Writes a list of a specific [Persistable] to the Bundle.
 *
 * This should be read with [getPersistableList].
 */
fun <P : Persistable> Bundle.putPersistableList(key: String?, list: List<P?>) =
  putPersistable(key, list, PersistableListSerializer)

/**
 * Reads a list of a specific [Persistable] from the Bundle.
 *
 * This should have been previously written using [putPersistableList].
 */
fun <P : Persistable> Bundle.getPersistableList(
  key: String,
  deserializer: PersistableDeserializer<P>,
): List<P?>? =
  getBundle(key)
    ?.toPersistableBundle()
    ?.let(PersistableListDeserializer(deserializer)::fromPersistableBundle)

/**
 * Put a map [value] to this [Bundle] under [key].
 *
 * @see getPersistableMap
 */
fun <K, V> Bundle.putPersistableMap(
  key: String,
  value: Map<K, V>?,
  keySerializer: NullablePersistableSerializer<K>,
  valueSerializer: NullablePersistableSerializer<V>,
) = putBundle(key, value?.let(PersistableMapSerializer(keySerializer, valueSerializer)::toBundle))

/**
 * Put a map [value] to this [Bundle] under [key].
 *
 * @see getPersistableMap
 */
fun <K : Persistable, V : Persistable> Bundle.putPersistableMap(key: String, map: Map<K, V>) =
  putPersistableMap(key, map, Persistable, Persistable)

/**
 * Get a Map<K,V> from this [Bundle] under [key].
 *
 * Assumes the value was written to via [putPersistableMap].
 *
 * @see putPersistableMap
 */
fun <K, V> Bundle.getPersistableMap(
  key: String?,
  keyDeserializer: PersistableDeserializer<K>,
  valueDeserializer: PersistableDeserializer<V>,
): Map<out K, V>? {
  return getBundle(key)
    ?.let(PersistableMapDeserializer(keyDeserializer, valueDeserializer)::fromBundle)
}

// Here we can add helpers for persistable wrapping for maps
// To add more, just copy a block of 4 and then add tests in BundleTests

fun <V : Persistable> Bundle.putPersistableMapWithStringKey(key: String, map: Map<String, V>) =
  putPersistableMap(key, map.mapKeys { PersistableString(it.key) })

fun <V : Persistable> Bundle.getPersistableMapWithStringKey(
  key: String,
  valueType: PersistableDeserializer<V>,
) = getPersistableMap(key, PersistableString, valueType)?.mapKeys { it.key.value }

fun <V : Persistable> Bundle.putPersistableMapWithIntKey(key: String, map: Map<Int, V>) =
  putPersistableMap(key, map.mapKeys { PersistableInt(it.key) })

fun <V : Persistable> Bundle.getPersistableMapWithIntKey(
  key: String,
  valueType: PersistableDeserializer<V>,
) = getPersistableMap(key, PersistableInt, valueType)?.mapKeys { it.key.value }

fun <V : Persistable> Bundle.putPersistableMapWithLongKey(key: String, map: Map<Long, V>) =
  putPersistableMap(key, map.mapKeys { PersistableLong(it.key) })

fun <V : Persistable> Bundle.getPersistableMapWithLongKey(
  key: String,
  valueType: PersistableDeserializer<V>,
) = getPersistableMap(key, PersistableLong, valueType)?.mapKeys { it.key.value }

// End section with helpers for persistable wrapping for maps

private fun generateArrayIndexedKey(key: String, index: Int) = "$key<$index>"

/** Key used to contain a Bundle or Persistable Bundle containing metadata about the conversion */
@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE) const val KEY_CONVERSION = "_conversion"

/**
 * Key used to contain a Bundle or Persistable Bundle mapping the keys which could not be stored
 * directly to their original types
 */
@VisibleForTesting(VisibleForTesting.PACKAGE_PRIVATE)
const val KEY_ORIGINAL_TYPES = "_original_types"

/** A shorthand for creating and initialising the [Bundle]. */
inline fun Bundle(init: Bundle.() -> Unit) = Bundle().apply(init)
