package com.android.onboarding.utils.persistable

import android.content.Intent

/** Writes the [Persistable] to the intent extras. */
fun Intent.putPersistableExtra(key: String, value: Persistable) {
  putExtra(key, value.toBundle())
}

/** Gets the [Persistable] with the given key. */
fun <T> Intent.getPersistableExtra(key: String, deserializer: PersistableDeserializer<T>): T? {
  val bundle = getBundleExtra(key) ?: return null
  return deserializer.fromPersistableBundle(bundle.toPersistableBundle())
}

/** A shorthand for initialising an [Intent]. */
inline fun Intent(action: String? = null, builder: Intent.() -> Unit): Intent =
  Intent().apply {
    action?.let(::setAction)
    builder()
  }
