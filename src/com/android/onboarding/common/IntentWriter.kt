package com.android.onboarding.common

import android.content.Intent
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RequiresApi
import java.io.Serializable

/** Utility object providing scoped DSL utilities for concise intent extra manipulation */
@Suppress("MemberVisibilityCanBePrivate")
object IntentWriter {
  fun Intent.intExtraOrNull(name: String): Int? = if (hasExtra(name)) getIntExtra(name, 0) else null

  fun Intent.intExtra(name: String): Int = intExtraOrNull(name) ?: missingExtraError(name)

  fun Intent.stringExtraOrNull(name: String): String? = getStringExtra(name)

  fun Intent.stringExtra(name: String): String = stringExtraOrNull(name) ?: missingExtraError(name)

  fun Intent.booleanExtraOrNull(name: String): Boolean? =
    if (hasExtra(name)) getBooleanExtra(name, false) else null

  fun Intent.booleanExtra(name: String): Boolean =
    booleanExtraOrNull(name) ?: missingExtraError(name)

  fun Intent.bundleExtraOrNull(name: String): Bundle? = getBundleExtra(name)

  fun Intent.bundleExtra(name: String): Bundle = bundleExtraOrNull(name) ?: missingExtraError(name)

  fun missingExtraError(name: String): Nothing = error("Required extra '$name' is missing")

  @RequiresApi(VERSION_CODES.TIRAMISU)
  inline fun <reified T> Intent.parcelableExtraOrNull(name: String): T? =
    getParcelableExtra(name, T::class.java)

  @RequiresApi(VERSION_CODES.TIRAMISU)
  inline fun <reified T> Intent.parcelableExtra(name: String): T =
    parcelableExtraOrNull(name) ?: missingExtraError(name)

  operator fun Intent.set(key: String, value: Boolean?) = apply { value?.let { putExtra(key, it) } }

  operator fun Intent.set(key: String, value: Byte?) = apply { value?.let { putExtra(key, it) } }

  operator fun Intent.set(key: String, value: Char?) = apply { value?.let { putExtra(key, it) } }

  operator fun Intent.set(key: String, value: Short?) = apply { value?.let { putExtra(key, it) } }

  operator fun Intent.set(key: String, value: Int?) = apply { value?.let { putExtra(key, it) } }

  operator fun Intent.set(key: String, value: Long?) = apply { value?.let { putExtra(key, it) } }

  operator fun Intent.set(key: String, value: Float?) = apply { value?.let { putExtra(key, it) } }

  operator fun Intent.set(key: String, value: Double?) = apply { value?.let { putExtra(key, it) } }

  operator fun Intent.set(key: String, value: String?) = putExtra(key, value)

  operator fun Intent.set(key: String, value: CharSequence?) = putExtra(key, value)

  operator fun Intent.set(key: String, value: Parcelable?) = putExtra(key, value)

  operator fun Intent.set(key: String, value: Bundle?) = putExtra(key, value)

  operator fun Intent.set(key: String, value: Serializable?) = putExtra(key, value)
}
