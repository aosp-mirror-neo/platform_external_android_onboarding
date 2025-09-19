package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import unsupported

// LINT.IfChange
inline fun PersistableBundle(init: PersistableBundle.() -> Unit) = PersistableBundle().apply(init)

fun PersistableBundle.putPersistable(key: String?, data: Persistable?) =
  putPersistable(key, data, Persistable)

fun <T> PersistableBundle.putPersistable(
  key: String?,
  value: T?,
  serializer: NullablePersistableSerializer<T>,
) = putPersistableBundle(key, value?.let(serializer::toPersistableBundle))

fun <T> PersistableBundle.getPersistable(
  key: String?,
  deserializer: PersistableDeserializer<T>,
): T? = getPersistableBundle(key)?.let(deserializer::fromPersistableBundle)

fun PersistableBundle.recursivelyRemoveKeys(matching: Set<String>): Unit = unsupported

fun <P : Persistable> PersistableBundle.putPersistableList(key: String?, value: List<P?>?): Unit =
  unsupported

fun <P> PersistableBundle.putPersistableList(
  key: String?,
  value: List<P?>?,
  serializer: NullablePersistableSerializer<P>,
): Unit = unsupported

fun <P> PersistableBundle.getPersistableList(
  key: String?,
  deserializer: PersistableDeserializer<P>,
): List<P?>? = unsupported

fun <P> PersistableBundle.getStrictPersistableList(
  key: String?,
  deserializer: PersistableDeserializer<P>,
): List<P & Any>? = unsupported
// LINT.ThenChange(//external/android_onboarding/java/com/android/onboarding/utils/persistable/PersistableBundle.kt)
