package com.android.onboarding.utils.persistable

import android.accounts.Account
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import android.util.Size
import androidx.core.net.toUri
import com.android.onboarding.compatibility.compatGetBoolean
import com.android.onboarding.compatibility.compatGetBooleanArray
import com.android.onboarding.compatibility.compatPutBoolean
import com.android.onboarding.compatibility.compatPutBooleanArray
import java.util.Base64
import java.util.Locale


/** A shorthand for creating and initialising the [PersistableBundle]. */
inline fun PersistableBundle(init: PersistableBundle.() -> Unit) = PersistableBundle().apply(init)

/** Writes a [Persistable] to the [PersistableBundle]. */
fun PersistableBundle.putPersistable(key: String?, data: Persistable?) =
  putPersistable(key, data, Persistable)

/** Writes a [Persistable] to the [PersistableBundle]. */
fun <T> PersistableBundle.putPersistable(
  key: String?,
  value: T?,
  serializer: NullablePersistableSerializer<T>,
) = putPersistableBundle(key, value?.let(serializer::toPersistableBundle))

/** Gets the [Persistable] with the given key. */
fun <T> PersistableBundle.getPersistable(
  key: String?,
  deserializer: PersistableDeserializer<T>,
): T? = getPersistableBundle(key)?.let(deserializer::fromPersistableBundle)

/**
 * Remove all keys in the given set from the [PersistableBundle] and from all [PersistableBundle]s
 * recursively inside the top-level [PersistableBundle].
 */
fun PersistableBundle.recursivelyRemoveKeys(matching: Set<String>) {
  for (k in matching) {
    remove(k)
  }
  for (key in keySet()) {
    getPersistableBundle(key)?.recursivelyRemoveKeys(matching)
  }
}

/**
 * Writes a list of a specific [Persistable] to the Bundle.
 *
 * This should be read with [getPersistableList].
 */
fun <P : Persistable> PersistableBundle.putPersistableList(key: String?, value: List<P?>?): Unit =
  putPersistableList(key, value, Persistable)

/**
 * Writes a list of a specific [Persistable] to the Bundle.
 *
 * This should be read with [getPersistableList].
 */
fun <P> PersistableBundle.putPersistableList(
  key: String?,
  value: List<P?>?,
  serializer: NullablePersistableSerializer<P>,
): Unit =
  putPersistableBundle(
    key,
    value?.let(PersistableListSerializer(serializer.withNullableValues())::toPersistableBundle),
  )

/**
 * Reads a list of a specific [Persistable] from the Bundle.
 *
 * This should have been previously written using [putPersistableList].
 */
fun <P> PersistableBundle.getPersistableList(
  key: String?,
  deserializer: PersistableDeserializer<P>,
): List<P?>? =
  getPersistableBundle(key)?.let(PersistableListDeserializer(deserializer)::fromPersistableBundle)

/**
 * Reads a list of a specific [Persistable] from the Bundle.
 *
 * This should have been previously written using [putPersistableList].
 *
 * Unlike [getPersistableList], this version asserts that each list element is not null and throws
 * [IllegalStateException] if any of them is.
 */
fun <P> PersistableBundle.getStrictPersistableList(
  key: String?,
  deserializer: PersistableDeserializer<P>,
): List<P & Any>? = getPersistableList(key, deserializer)?.map(::checkNotNull)



/**
 * Returns a new [Bundle] containing all values in the [PersistableBundle].
 *
 * If a [KEY_CONVERSION] key is present, it will be used to map data back to their original types
 * where possible and will then be removed from the resulting [Bundle]. This means when all data is
 * persistable, [Bundle.toPersistableBundle] followed by [PersistableBundle.toBundle] is no-op.
 */
fun PersistableBundle.toBundle(onError: ((key: String, error: String) -> Unit)? = null): Bundle {
  val bundle = Bundle(this)
  val originalTypes =
    getPersistableBundle(KEY_CONVERSION)?.getPersistableBundle(KEY_ORIGINAL_TYPES)
      ?: PersistableBundle()

  for (key in originalTypes.keySet()) {
    val value = bundle.get(key)

    if (value == null) {
      // This means this data was dropped
      onError?.invoke(
        key,
        "$key existed in the original Bundle but was dropped moving to PersistableBundle",
      )
      continue
    }

    val originalType = originalTypes.getString(key)?.split("<")?.get(0)

    if (value::class.qualifiedName != originalType) {
      // It was converted...
      when (originalType) {
        Byte::class.qualifiedName -> bundle.putByte(key, (value as Int).toByte())
        ByteArray::class.qualifiedName ->
          bundle.putByteArray(key, (value as IntArray).map { it.toByte() }.toByteArray())
        Short::class.qualifiedName -> bundle.putShort(key, (value as Int).toShort())
        ShortArray::class.qualifiedName ->
          bundle.putShortArray(key, (value as IntArray).map { it.toShort() }.toShortArray())
        Char::class.qualifiedName -> bundle.putChar(key, (value as String).subSequence(0, 1)[0])
        CharArray::class.qualifiedName ->
          bundle.putCharArray(
            key,
            (value as Array<String>).map { it.subSequence(0, 1)[0] }.toCharArray(),
          )
        ComponentName::class.qualifiedName ->
          bundle.putParcelable(key, ComponentName.unflattenFromString(value as String))
        Account::class.qualifiedName ->
          parseAccount((value as PersistableBundle))?.let { bundle.putParcelable(key, it) }
        Locale::class.qualifiedName ->
          bundle.putSerializable(key, Locale.forLanguageTag(value as String))
        Uri::class.qualifiedName -> bundle.putParcelable(key, (value as String).toUri())
        // CharSequence and String are interchangeable in Bundle so extracting as a String is fine
        Float::class.qualifiedName -> bundle.putFloat(key, (value as Double).toFloat())
        FloatArray::class.qualifiedName ->
          bundle.putFloatArray(key, (value as DoubleArray).map { it.toFloat() }.toFloatArray())
        Size::class.qualifiedName -> bundle.putSize(key, Size.parseSize(value as String))
        Bundle::class.qualifiedName ->
          bundle.putBundle(key, (value as PersistableBundle).toBundle(onError))
        Boolean::class.qualifiedName -> bundle.compatPutBoolean(key, compatGetBoolean(key))
        BooleanArray::class.qualifiedName ->
          bundle.compatPutBooleanArray(
            key,
            compatGetBooleanArray(key) ?: error("Missing BoolenArray"),
          )

        ArrayList::class.qualifiedName -> {
          val arrayListType = originalTypes.getString(key, "").split("<")[1].split(">")[0]

          when (arrayListType) {
            Int::class.qualifiedName ->
              bundle.putIntegerArrayList(key, ArrayList((value as IntArray).toList()))
            // String and Charsequence are interchangeable in Bundles
            String::class.qualifiedName ->
              bundle.putCharSequenceArrayList(key, ArrayList((value as Array<String>).toList()))
            else ->
              onError?.invoke(
                key,
                "$key originally had array list type $originalType but we cannot convert back to that",
              )
          }
        }

        Array::class.qualifiedName -> {
          val arrayType = originalTypes.getString(key, "").split("<")[1].split(">")[0]

          when (arrayType) {
            CharSequence::class.qualifiedName ->
              bundle.putCharSequenceArray(key, value as Array<String>)
            Bundle::class.qualifiedName -> {
              bundle.putParcelableArray(
                key,
                getPersistableList(key, PersistablePersistableBundle)
                  ?.map { it?.persistableBundle?.toBundle(onError) }
                  ?.toTypedArray(),
              )
            }
            else ->
              onError?.invoke(
                key,
                "$key originally had array type $originalType but we cannot convert back to that",
              )
          }
        }
        Intent::class.qualifiedName -> {
          val persistableIntent =
            PersistableIntent.fromPersistableBundle(value as PersistableBundle)
          bundle.putParcelable(key, persistableIntent.toIntent())
        }
        else ->
          onError?.invoke(
            key,
            "$key originally had type $originalType but we cannot convert back to that",
          )
      }
    }
  }

  bundle.remove(KEY_CONVERSION)

  return bundle
}

/**
 * Return a new PersistableBundle which is the result of parceling and unparceling this
 * PersistableBundle.
 */
fun PersistableBundle.parcelAndUnparcel(): PersistableBundle {
  val p = Parcel.obtain()
  try {
    p.writePersistableBundle(this)
    p.setDataPosition(0)
    return p.readPersistableBundle(javaClass.classLoader)
      ?: error("Should never happen... we just wrote a persistable bundle and can't read it back")
  } finally {
    p.recycle()
  }
}

/**
 * Puts a [Parcelable] value into the [PersistableBundle].
 *
 * This is dangerous, as [Parcelable] objects are not always persistable. Additionally, "changes in
 * the underlying implementation of any of the data in the Parcel can render older data unreadable"
 * (from the [Parcel] documentation).
 *
 * It is generally okay if the [PersistableBundle] is not going to be written to disk, or will only
 * survive for the lifetime of the process, etc. (situations where the [Parcelable] implementations
 * cannot be changed between the storing and the retrieving).
 *
 * [unsafeGetParcelable] should be used to read this value.
 */
fun PersistableBundle.unsafePutParcelable(key: String, parcelable: Parcelable) {
  val p = Parcel.obtain()
  parcelable.writeToParcel(p, /* flags= */ 0)
  val bytes = p.marshall()
  putString(key, Base64.getEncoder().encodeToString(bytes))
  p.recycle()
}

/**
 * Reads a [Parcelable] written using [unsafePutParcelable].
 *
 * If no value is available, then null will be returned. If invalid data is present then an
 * [IllegalArgumentException] will be thrown.
 *
 * See [unsafePutParcelable] for details on storing [Parcelable] in [PersistableBundle].
 */
fun <T : Parcelable> PersistableBundle.unsafeGetParcelable(
  key: String,
  creator: Parcelable.Creator<T>,
): T? {
  val p = Parcel.obtain()
  val str = getString(key) ?: return null
  try {
    val bytes = Base64.getDecoder().decode(str)
    p.unmarshall(bytes, 0, bytes.size)
    p.setDataPosition(0)
    val obj = creator.createFromParcel(p)
    return obj
  } catch (t: Throwable) {
    throw IllegalArgumentException("Could not deserialize data at key $key", t)
  } finally {
    p.recycle()
  }
}

fun <K : Persistable, V : Persistable> PersistableBundle.putPersistableMap(
  key: String?,
  value: Map<K, V>?,
) = putPersistableMap(key, value, Persistable, Persistable)

fun <K, V> PersistableBundle.putPersistableMap(
  key: String?,
  value: Map<K, V>?,
  keySerializer: NullablePersistableSerializer<K>,
  valueSerializer: NullablePersistableSerializer<V>,
) =
  putPersistableBundle(
    key,
    value?.let(PersistableMapSerializer(keySerializer, valueSerializer)::toPersistableBundle),
  )

fun <K, V> PersistableBundle.getPersistableMap(
  key: String?,
  keyType: PersistableDeserializer<K>,
  valueType: PersistableDeserializer<V>,
): Map<out K, V>? =
  getPersistableBundle(key)
    ?.let(PersistableMapDeserializer(keyType, valueType)::fromPersistableBundle)

// Here we can add helpers for persistable wrapping for maps
// To add more, just copy a block of 4 and then add tests in BundleTests

// String -> Persistable
fun <V : Persistable> PersistableBundle.putPersistableMapWithStringKey(
  key: String,
  map: Map<String, V>,
) = putPersistableMap(key, map.mapKeys { PersistableString(it.key) })

fun <V : Persistable> PersistableBundle.getPersistableMapWithStringKey(
  key: String,
  valueType: PersistableDeserializer<V>,
) = getPersistableMap(key, PersistableString, valueType)?.mapKeys { it.key.value }

// Int -> Persistable
fun <V : Persistable> PersistableBundle.putPersistableMapWithIntKey(key: String, map: Map<Int, V>) =
  putPersistableMap(key, map.mapKeys { PersistableInt(it.key) })

fun <V : Persistable> PersistableBundle.getPersistableMapWithIntKey(
  key: String,
  valueType: PersistableDeserializer<V>,
) = getPersistableMap(key, PersistableInt, valueType)?.mapKeys { it.key.value }

// Long -> Persistable
fun <V : Persistable> PersistableBundle.putPersistableMapWithLongKey(
  key: String,
  map: Map<Long, V>,
) = putPersistableMap(key, map.mapKeys { PersistableLong(it.key) })

fun <V : Persistable> PersistableBundle.getPersistableMapWithLongKey(
  key: String,
  valueType: PersistableDeserializer<V>,
) = getPersistableMap(key, PersistableLong, valueType)?.mapKeys { it.key.value }

fun PersistableBundle.sizeInBytes(): Int {
  val p = Parcel.obtain()
  val size =
    try {
      p.writePersistableBundle(this)
      p.dataSize()
    } finally {
      p.recycle()
    }
  return size
}

fun PersistableBundle.toByteArray(): ByteArray {
  val p = Parcel.obtain()
  val fullDataByteArray =
    try {
      p.writePersistableBundle(this)
      p.marshall()
    } finally {
      p.recycle()
    }
  return fullDataByteArray
}
