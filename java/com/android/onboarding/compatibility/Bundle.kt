package com.android.onboarding.compatibility

import android.os.BaseBundle
import android.os.Build
import android.os.Bundle
import android.os.Parcelable

/**
 * Backwards-compatible version of [Bundle.putBoolean].
 *
 * Prior to SDK 22, this will store an int with a value of 1 for true and 0 for false. These values
 * will be converted back to booleans using [compatGetBoolean].
 */
fun BaseBundle.compatPutBoolean(key: String?, value: Boolean) {
  if (Build.VERSION.SDK_INT >= 22) {
    putBoolean(key, value)
  } else {
    putInt(key, if (value) 1 else 0)
  }
}

/**
 * Backwards-compatible version of [Bundle.putBoolean].
 *
 * Prior to SDK 22, this will store an int with a value of 1 for true and 0 for false. These values
 * will be converted back to booleans using [compatGetBoolean].
 */
fun BaseBundle.compatPutBooleanArray(key: String?, value: BooleanArray?) {
  if (Build.VERSION.SDK_INT >= 22) {
    putBooleanArray(key, value)
  } else {
    putIntArray(key, value?.map { if (it) 1 else 0 }?.toIntArray())
  }
}

/**
 * Backwards-compatible version of [Bundle.getBoolean].
 *
 * This can parse booleans set using [Bundle.putBoolean] or [Bundle.compatPutBoolean].
 */
@JvmOverloads
fun BaseBundle.compatGetBoolean(key: String?, defaultValue: Boolean = false) =
  when (val value = get(key)) {
    is Boolean -> value
    is Int -> value == 1
    else -> defaultValue
  }

/**
 * Backwards-compatible version of [Bundle.getBooleanArray].
 *
 * This can parse booleans set using [Bundle.putBoolean] or [Bundle.compatPutBoolean].
 */
fun BaseBundle.compatGetBooleanArray(key: String?) =
  when (val value = get(key)) {
    is BooleanArray -> value
    is IntArray -> value.map { it == 1 }.toBooleanArray()
    else -> null
  }

/** @see Bundle.getParcelable */
inline fun <reified T : Parcelable> Bundle.compatGetParcelable(key: String?): T? =
  compatGetParcelable(key, T::class.java)

/** @see Bundle.getParcelable */
fun <T : Parcelable> Bundle.compatGetParcelable(key: String?, klass: Class<T>): T? =
  if (Build.VERSION.SDK_INT >= 33) {
    getParcelable(key, klass)
  } else {
    val oldClassLoader = classLoader
    classLoader = klass.classLoader
    @Suppress("DEPRECATION") getParcelable<T>(key).also { classLoader = oldClassLoader }
  }

/** @see Bundle.getParcelableArrayList */
inline fun <reified T : Parcelable> Bundle.compatGetParcelableArrayList(
  key: String?
): ArrayList<T?>? = compatGetParcelableArrayList(key, T::class.java)

/** @see Bundle.getParcelableArrayList */
fun <T : Parcelable> Bundle.compatGetParcelableArrayList(
  key: String?,
  klass: Class<T>,
): ArrayList<T?>? {
  val oldClassLoader = classLoader
  classLoader = klass.classLoader
  return try {
    if (Build.VERSION.SDK_INT >= 33) {
      getParcelableArrayList(key, klass)
    } else {
      @Suppress("DEPRECATION") getParcelableArrayList<T>(key)
    }
  } finally {
    classLoader = oldClassLoader
  }
}

/** @see Bundle.getParcelableArray */
inline fun <reified T : Parcelable> Bundle.compatGetParcelableArray(key: String?): Array<T?>? =
  compatGetParcelableArray(key, T::class.java)

/** @see Bundle.getParcelableArray */
fun <T : Parcelable> Bundle.compatGetParcelableArray(key: String?, klass: Class<T>): Array<T?>? {
  val oldClassLoader = classLoader
  classLoader = klass.classLoader
  return try {
    val list =
      if (Build.VERSION.SDK_INT >= 33) {
        getParcelableArray(key, klass)
      } else {
        @Suppress("DEPRECATION") getParcelableArray(key)
      } ?: return null
    @Suppress("UNCHECKED_CAST")
    val typedArray = java.lang.reflect.Array.newInstance(klass, list.size) as Array<T?>
    for ((i, it) in list.withIndex()) {
      @Suppress("UNCHECKED_CAST")
      typedArray[i] = it as T?
    }
    typedArray
  } finally {
    classLoader = oldClassLoader
  }
}
