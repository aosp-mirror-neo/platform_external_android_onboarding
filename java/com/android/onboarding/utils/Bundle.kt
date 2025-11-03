package com.android.onboarding.utils

import android.os.BaseBundle
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray

/**
 * Generates a useful hashcode for collision checks.
 *
 * Compatible with [contentDeepEquals].
 *
 * @param includeUnderscoredKeys should all keys starting with underscore be included. These usually
 *   are ignored as they contain metadata which is not part of the underlying data being passed
 *   around.
 */
fun BaseBundle?.generateHash(includeUnderscoredKeys: Boolean = false): Int {
  if (this == null) return 0
  val keySet =
    if (includeUnderscoredKeys) {
      keySet()
    } else {
      keySet().filterNot { it.startsWith("_") }
    }
  var result = keySet.size
  for (key in keySet.sorted()) {
    val value = get(key)
    val valueHash = getHashCodeOf(value, includeUnderscoredKeys)
    result = (31 * (31 * result) + key.hashCode()) + valueHash
  }

  return result
}

private fun getHashCodeOf(value: Any?, includeUnderscoredKeys: Boolean) =
  when (value) {
    is BaseBundle -> value.generateHash(includeUnderscoredKeys)
    is IntArray -> value.contentHashCode()
    is LongArray -> value.contentHashCode()
    is DoubleArray -> value.contentHashCode()
    is BooleanArray -> value.contentHashCode()
    is ByteArray -> value.contentHashCode()
    is ShortArray -> value.contentHashCode()
    is CharArray -> value.contentHashCode()
    is FloatArray -> value.contentHashCode()
    is Array<*> -> value.contentHashCode()
    is SparseArray<*> -> sparseArrayContentHashCode(value, includeUnderscoredKeys)
    else -> value.hashCode()
  }

private fun sparseArrayContentHashCode(
  sparseArray: SparseArray<*>,
  includeUnderscoredKeys: Boolean,
): Int {
  var hash = 0
  val size = sparseArray.size()

  for (index in 0 until size) {
    val key = sparseArray.keyAt(index)
    val value = sparseArray.valueAt(index)
    hash = 31 * hash + key
    hash = 31 * hash + getHashCodeOf(value, includeUnderscoredKeys)
  }
  return hash
}

/**
 * Performs a deep content equality check.
 *
 * Compatible with [generateHash].
 */
infix fun BaseBundle?.contentDeepEquals(other: Any?): Boolean =
  contentDeepEquals(other, includeUnderscoredKeys = false)

/**
 * Performs a deep content equality check.
 *
 * Compatible with [generateHash].
 *
 * @param includeUnderscoredKeys should all keys starting with underscore be included. These usually
 *   are ignored as they contain metadata which is not part of the underlying data being passed
 *   around.
 */
fun BaseBundle?.contentDeepEquals(other: Any?, includeUnderscoredKeys: Boolean): Boolean {
  if (this == null) return other == null
  if (other !is BaseBundle) {
    return false
  }

  val keySet =
    if (includeUnderscoredKeys) {
      keySet()
    } else {
      keySet().filterNot { it.startsWith("_") }
    }
  val otherKeySet =
    if (includeUnderscoredKeys) {
      other.keySet()
    } else {
      other.keySet().filterNot { it.startsWith("_") }
    }

  if (otherKeySet.size != keySet.size) {
    return false
  }

  if (!otherKeySet.containsAll(keySet)) {
    return false
  }

  for (key in keySet) {
    val value = get(key)
    val otherValue = other.get(key)

    if (value == null) {
      if (otherValue != null) {
        return false
      }
    } else if (otherValue == null) {
      return false
    } else {
      if (value::class != otherValue::class) {
        return false
      }

      if (!valuesMatch(value, otherValue, includeUnderscoredKeys)) {
        return false
      }
    }
  }

  return true
}

/**
 * Write this [BaseBundle] to a string recursively listing all keys with their value types without
 * printing the actual content.
 */
@JvmOverloads
fun BaseBundle.toStringNoContent(spaces: Boolean = false): String {
  val sep = if (spaces) " " else ""
  val name = this::class.simpleName ?: this::class.java.superclass?.simpleName
  return keySet().joinToString(prefix = "$name{", separator = ",$sep", postfix = "}") { key ->
    when (@Suppress("DEPRECATION") val value = get(key)) {
      null -> "$key:${sep}null"
      is BaseBundle -> "$key:$sep${value.toStringNoContent(spaces)}"
      else -> "$key:$sep${value::class.qualifiedName}"
    }
  }
}

/**
 * Forces a [BaseBundle] and all values inside it to be unparcelled.
 *
 * Without doing this, equality and toString will not work correctly.
 */
private fun BaseBundle.forceUnparcel() {
  for (key in keySet()) {
    @Suppress("DEPRECATION") val o = get(key)
    if (o is BaseBundle) {
      o.forceUnparcel()
    }
  }
}

/**
 * Version of [toString] which forces the bundle to resolve parceled data so at least all keys are
 * visible.
 *
 * This is more expensive than [toString] as it loops recursively through everything in the bundle.
 *
 * Note that, as this depends on the framework [toString] method, it has significantly more useful
 * output on SDK 29+.
 */
fun BaseBundle.forceToString(): String =
  keySet().joinToString(
    prefix = "${this@forceToString::class.simpleName}{",
    separator = ", ",
    postfix = "}",
  ) { key ->
    @Suppress("DEPRECATION") // Needed for generic access
    val strVal = transformToBundleString(get(key))
    "$key=$strVal"
  }

private fun transformToBundleString(value: Any?): String =
  when (value) {
    is BaseBundle -> value.forceToString()
    is ByteArray -> "ByteArray(hash=${value.contentHashCode()})"
    is IntArray ->
      value.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
        transform = ::transformToBundleString,
      )
    is LongArray ->
      value.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
        transform = ::transformToBundleString,
      )
    is DoubleArray ->
      value.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
        transform = ::transformToBundleString,
      )
    is BooleanArray ->
      value.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
        transform = ::transformToBundleString,
      )
    is ShortArray ->
      value.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
        transform = ::transformToBundleString,
      )
    is CharArray ->
      value.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
        transform = ::transformToBundleString,
      )
    is FloatArray ->
      value.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
        transform = ::transformToBundleString,
      )
    is Array<*> ->
      value.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
        transform = ::transformToBundleString,
      )
    is Iterable<*> ->
      value.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]",
        transform = ::transformToBundleString,
      )
    else -> value.toString()
  }

private fun valuesMatch(value: Any, otherValue: Any, includeUnderscoredKeys: Boolean): Boolean {
  return when (value) {
    is BaseBundle -> value.contentDeepEquals(otherValue as BaseBundle, includeUnderscoredKeys)
    is Array<*> -> value contentDeepEquals (otherValue as Array<*>)
    is IntArray -> value contentEquals (otherValue as IntArray)
    is LongArray -> value contentEquals (otherValue as LongArray)
    is DoubleArray -> value contentEquals (otherValue as DoubleArray)
    is BooleanArray -> value contentEquals (otherValue as BooleanArray)
    is ByteArray -> value contentEquals (otherValue as ByteArray)
    is ShortArray -> value contentEquals (otherValue as ShortArray)
    is CharArray -> value contentEquals (otherValue as CharArray)
    is FloatArray -> value contentEquals (otherValue as FloatArray)
    else -> value == otherValue
  }
}

/** Writes a [Parcelable] to a [Bundle] by converting it to a [ByteArray]. */
fun Bundle?.putAsByteArray(key: String, parcelable: Parcelable) {
  if (this == null) return // No-op on null to preserve legacy behaviour
  val p = Parcel.obtain()
  p.writeParcelable(parcelable, 0 /* flags */)
  putByteArray(key, p.marshall())
  p.recycle()
}

/** Reads a [Parcelable] previously written using [Bundle.putAsByteArray]. */
fun <T : Parcelable> Bundle?.getFromByteArray(key: String, cls: Class<T>): T? {
  if (this == null) return null // Preserve legacy behaviour
  val bytes: ByteArray = getByteArray(key) ?: return null
  val p = Parcel.obtain()
  p.unmarshall(bytes, 0, bytes.size)
  p.setDataPosition(0)
  val result: T? = p.readParcelable(cls.getClassLoader())
  p.recycle()
  return result
}
