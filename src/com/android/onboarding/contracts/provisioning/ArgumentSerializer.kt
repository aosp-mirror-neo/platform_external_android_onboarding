package com.android.onboarding.contracts.provisioning

import android.content.Intent
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RequiresApi
import java.io.Serializable

/** Utility parent class providing scoped DSL utilities for concise intent extra manipulation */
@Suppress("MemberVisibilityCanBePrivate")
abstract class ArgumentSerializer {
  protected fun Intent.intExtraOrNull(name: String): Int? =
    if (hasExtra(name)) getIntExtra(name, 0) else null

  protected fun Intent.intExtra(name: String): Int = intExtraOrNull(name) ?: missingExtraError(name)

  protected fun Intent.stringExtraOrNull(name: String): String? = getStringExtra(name)

  protected fun Intent.stringExtra(name: String): String =
    stringExtraOrNull(name) ?: missingExtraError(name)

  protected fun Intent.booleanExtraOrNull(name: String): Boolean? =
    if (hasExtra(name)) getBooleanExtra(name, false) else null

  protected fun Intent.booleanExtra(name: String): Boolean =
    booleanExtraOrNull(name) ?: missingExtraError(name)

  protected fun Intent.bundleExtraOrNull(name: String): Bundle? = getBundleExtra(name)

  protected fun Intent.bundleExtra(name: String): Bundle =
    bundleExtraOrNull(name) ?: missingExtraError(name)

  protected fun missingExtraError(name: String): Nothing =
    error("Required extra '$name' is missing")

  @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
  protected inline fun <reified T> Intent.parcelableExtraOrNull(name: String): T? =
    getParcelableExtra(name, T::class.java)

  @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
  protected inline fun <reified T> Intent.parcelableExtra(name: String): T =
    parcelableExtraOrNull(name) ?: missingExtraError(name)

  protected operator fun Intent.set(key: String, value: Boolean) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Byte) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Char) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Short) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Int) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Long) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Float) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Double) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: String?) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: CharSequence?) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Parcelable?) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Bundle?) = putExtra(key, value)

  protected operator fun Intent.set(key: String, value: Serializable?) = putExtra(key, value)
}
