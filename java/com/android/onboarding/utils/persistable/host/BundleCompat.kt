package com.android.onboarding.compatibility

import android.os.BaseBundle
import unsupported

/**
 * Backwards-compatible version of [Bundle.putBoolean].
 *
 * Prior to SDK 22, this will store an int with a value of 1 for true and 0 for false. These values
 * will be converted back to booleans using [compatGetBoolean].
 */
fun BaseBundle.compatPutBoolean(key: String?, value: Boolean): Unit = unsupported

/**
 * Backwards-compatible version of [Bundle.putBoolean].
 *
 * Prior to SDK 22, this will store an int with a value of 1 for true and 0 for false. These values
 * will be converted back to booleans using [compatGetBoolean].
 */
fun BaseBundle.compatPutBooleanArray(key: String?, value: BooleanArray?): Unit = unsupported

/**
 * Backwards-compatible version of [Bundle.getBoolean].
 *
 * This can parse booleans set using [Bundle.putBoolean] or [Bundle.compatPutBoolean].
 */
@JvmOverloads
fun BaseBundle.compatGetBoolean(key: String?, defaultValue: Boolean = false): Boolean = unsupported
