package com.android.onboarding.common.annotations

/**
 * Attaches a unique key to a class.
 *
 * This will be enforced at compile time that all keys are unique for a given type of key.
 */

annotation class UniqueKey(val key: String)

/** Get the key for the given class, or null if there isn't one. */
fun Class<*>.getUniqueKey(): String? {
  return getAnnotation(UniqueKey::class.java)?.key
}
