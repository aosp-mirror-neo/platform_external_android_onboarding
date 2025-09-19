package com.android.onboarding.utils.persistable

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality
import java.io.ByteArrayOutputStream
import java.util.Base64

/** Wrapper around [Bitmap] to make it [Persistable]. */
data class PersistableBitmap(val value: Bitmap) : Persistable, StableEquality {
  override fun toPersistableBundle() = toPersistableBundle(value)

  // There is no value in showing a random number for the actual bitmap
  override fun toString(): String = "PersistableBitmap"

  override fun equals(other: Any?) =
    other is PersistableBitmap && toByteString(value) == toByteString(other.value)

  override fun hashCode() = toByteString(value).hashCode()

  companion object : PersistableDeserializer<PersistableBitmap>, PersistableSerializer<Bitmap> {
    private const val KEY = "value"

    private fun fromByteString(string: String): PersistableBitmap {
      val data = Base64.getDecoder().decode(string)
      val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
      return PersistableBitmap(bitmap)
    }

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      fromByteString(persistableBundle.getString(KEY) ?: error("PersistableBitmap has no value"))

    override fun toPersistableBundle(value: Bitmap) = PersistableBundle {
      putString(KEY, toByteString(value))
    }

    private fun toByteString(value: Bitmap): String {
      val byteStream = ByteArrayOutputStream()
      value.compress(Bitmap.CompressFormat.PNG, 0, byteStream)
      val bitmapBytes = byteStream.toByteArray()

      return String(Base64.getEncoder().encode(bitmapBytes))
    }
  }
}
