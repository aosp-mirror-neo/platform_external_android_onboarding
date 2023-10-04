package com.android.onboarding.contracts

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.RequiresApi

/** Interface for writing an object to an [Intent] and reading the same object from an [Intent]. */
interface IntentSerializer<V> {
  fun write(intent: Intent, value: V)

  fun read(intent: Intent): V
}

/**
 * A serializer that does not expose [Intent] directly and instead works via [IntentScope]
 * abstraction that can be observed
 *
 * TODO This is living as a separate opt-in interface for now, but we should look into incorporating
 * it inside the contract class to make methods enforceable and protected
 */
interface ScopedIntentSerializer<V> : IntentSerializer<V> {
  override fun write(intent: Intent, value: V) = IntentScope(intent).write(value)

  override fun read(intent: Intent): V = IntentScope(intent).read()

  fun IntentScope.write(value: V)

  /** Writes a [value] to the [IntentScope] via a given [serializer] */
  fun <T> IntentScope.write(serializer: ScopedIntentSerializer<T>, value: T): Unit =
    with(serializer) { write(value) }

  fun IntentScope.read(): V

  /** Reads a value [T] from the [IntentScope] via a given [serializer] */
  fun <T> IntentScope.read(serializer: ScopedIntentSerializer<T>): T = with(serializer) { read() }
}

/**
 * An observable abstraction over [Intent] extras.
 *
 * @param androidIntent to wrap
 * @param afterRead function to call after each read
 * @param beforeWrite function to call before each write
 */
@IntentScope.IntentManipulationDsl
data class IntentScope(
  @PublishedApi internal val androidIntent: Intent,
  @PublishedApi internal val afterRead: (key: String, value: Any?) -> Unit = { _, _ -> },
  @PublishedApi internal val beforeWrite: (key: String, value: Any?) -> Unit = { _, _ -> },
) {
  /** Marks functions that read or write intent data */
  @DslMarker internal annotation class IntentManipulationDsl

  /**
   * Self-reference for more fluid write access
   *
   * ```
   * with(IntentScope) {
   *   intent[KEY]= "value"
   * }
   * ```
   */
  @IntentManipulationDsl val intent: IntentScope = this

  @IntentManipulationDsl operator fun contains(key: String): Boolean = androidIntent.hasExtra(key)

  @IntentManipulationDsl
  fun intExtraOrNull(key: String): Int? =
    run { if (contains(key)) androidIntent.getIntExtra(key, 0) else null }
      .also { afterRead(key, it) }

  @IntentManipulationDsl
  fun intExtra(key: String): Int = intExtraOrNull(key) ?: missingExtraError(key)

  @IntentManipulationDsl
  fun stringExtraOrNull(key: String): String? =
    androidIntent.getStringExtra(key).also { afterRead(key, it) }

  @IntentManipulationDsl
  fun stringExtra(key: String): String = stringExtraOrNull(key) ?: missingExtraError(key)

  @IntentManipulationDsl
  fun booleanExtraOrNull(key: String): Boolean? =
    run { if (contains(key)) androidIntent.getBooleanExtra(key, false) else null }
      .also { afterRead(key, it) }

  @IntentManipulationDsl
  fun booleanExtra(key: String): Boolean = booleanExtraOrNull(key) ?: missingExtraError(key)

  @IntentManipulationDsl
  fun bundleExtraOrNull(key: String): Bundle? =
    androidIntent.getBundleExtra(key).also { afterRead(key, it) }

  @IntentManipulationDsl
  fun bundleExtra(key: String): Bundle = bundleExtraOrNull(key) ?: missingExtraError(key)

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  @IntentManipulationDsl
  inline fun <reified T> parcelableExtraOrNull(key: String): T? =
    androidIntent.getParcelableExtra(key, T::class.java).also { afterRead(key, it) }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  @IntentManipulationDsl
  inline fun <reified T> parcelableExtra(key: String): T =
    parcelableExtraOrNull(key) ?: missingExtraError(key)

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  @IntentManipulationDsl
  inline fun <reified T> parcelableArrayExtraOrNull(key: String): Array<T>? =
    androidIntent.getParcelableArrayExtra(key, T::class.java).also { afterRead(key, it) }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  @IntentManipulationDsl
  inline fun <reified T> parcelableArrayExtra(key: String): Array<T> =
    parcelableArrayExtraOrNull(key) ?: missingExtraError(key)

  @IntentManipulationDsl
  fun missingExtraError(key: String): Nothing = error("Required extra '$key' is missing")

  @IntentManipulationDsl
  operator fun set(key: String, value: Boolean?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, it) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Byte?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, it) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Char?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, it) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Short?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, it) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Int?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, it) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Long?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, it) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Float?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, it) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Double?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, it) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: String?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, value) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: CharSequence?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, value) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Parcelable?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, value) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Array<out Parcelable>?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, value) }
  }

  @IntentManipulationDsl
  operator fun set(key: String, value: Bundle?): IntentScope = apply {
    beforeWrite(key, value)
    value?.let { androidIntent.putExtra(key, value) }
  }
}
