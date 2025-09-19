package com.android.onboarding.testing

import android.content.Intent
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDelegate
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableSerializer

/**
 * Writes [input] to an [Intent] extra, then writes that [Intent] to [Parcel] and finally reads
 * everything back from the [Parcel].
 */
inline fun <reified T : Parcelable> reparcelize(input: T): T {
  val key = "com.android.onboarding.reparcelize.DATA"
  val subIntent = Intent()
  subIntent.putExtra(key, input)
  val parcel = Parcel.obtain()
  subIntent.writeToParcel(parcel, 0)

  parcel.setDataPosition(0)
  val reparcelizedIntent = Intent.CREATOR.createFromParcel(parcel)
  val actual =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      @Suppress("DEPRECATION") reparcelizedIntent.getParcelableExtra(key)
    } else {
      reparcelizedIntent.getParcelableExtra(key, T::class.java)
    }
  return checkNotNull(actual)
}

/**
 * Writes [input] to a [PersistableBundle] extra, then writes that [PersistableBundle] to
 * [File][java.io.File] and finally reads everything back from the [File][java.io.File] and returns
 * the resulting [PersistableBundle].
 */
fun <T : Persistable> repersist(input: T, deserializer: PersistableDeserializer<T>): T =
  repersist(input, Persistable::toPersistableBundle, deserializer)

/**
 * Writes [input] to a [PersistableBundle] extra, then writes that [PersistableBundle] to
 * [File][java.io.File] and finally reads everything back from the [File][java.io.File] and returns
 * the resulting [PersistableBundle].
 */
fun <T> repersist(input: T, delegate: PersistableDelegate<T>): T =
  repersist(input, delegate, delegate)

/**
 * Writes [input] to a [PersistableBundle] extra, then writes that [PersistableBundle] to
 * [File][java.io.File] and finally reads everything back from the [File][java.io.File] and returns
 * the resulting [PersistableBundle].
 */
fun <V, T> repersist(
  input: V,
  serializer: PersistableSerializer<V>,
  deserializer: PersistableDeserializer<T>,
): T {
  val key = "com.android.onboarding.repersist.DATA"
  val subBundle = PersistableBundle()
  subBundle.putPersistableBundle(key, serializer.toPersistableBundle(input))

  val parcel = Parcel.obtain()

  val actual =
    try {
      subBundle.writeToParcel(parcel, 0)
      parcel.setDataPosition(0)
      parcel.readPersistableBundle(serializer::class.java.classLoader)
    } finally {
      parcel.recycle()
    }
  return checkNotNull(actual)
    .getPersistableBundle(key)
    .let(::checkNotNull)
    .let(deserializer::fromPersistableBundle)
}
