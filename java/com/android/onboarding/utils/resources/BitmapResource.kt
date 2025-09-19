package com.android.onboarding.utils.resources

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.graphics.drawable.toDrawable
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableBitmap
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable

/** A [Bitmap] which can either be provided directly or reference a drawable resource. */
data class BitmapResource
internal constructor(
  // Because of the constructors one of these will be non-null
  private val value: PersistableBitmap?,
  // This is a pair of packageName, resourceId
  @get:VisibleForTesting val resourceReference: Pair<String, Int>?,
) : Persistable, StableEquality {

  constructor(bitmap: Bitmap) : this(PersistableBitmap(value = bitmap), resourceReference = null)

  constructor(
    context: Context,
    resourceId: Int,
  ) : this(packageName = context.packageName, resourceId = resourceId)

  private constructor(
    packageName: String,
    resourceId: Int,
  ) : this(value = null, resourceReference = packageName to resourceId)

  private var hasCached: Boolean = false
  private var cachedValue: Bitmap? = null

  override fun toString(): String {
    if (value != null) {
      return "BitmapResource{value=$value}"
    }
    return "BitmapResource{package=${resourceReference!!.first}, resourceId=${resourceReference.second}"
  }

  /**
   * Resolves the [Bitmap] represented by this resource.
   *
   * If the resource is not available or is not convertible to a [Bitmap] then null will be
   * returned.
   */
  fun get(context: Context): Bitmap? {
    if (!hasCached) {
      cachedValue =
        value?.value
          ?: getBitmapFromResId(context, resourceReference!!.first, resourceReference.second)
      hasCached = true
    }

    return cachedValue
  }

  /**
   * Resolves the [Bitmap] represented by this resource.
   *
   * If the resource is not available or is not convertible to a [Bitmap] then [default] will be
   * returned.
   */
  fun get(context: Context, default: Bitmap): Bitmap = get(context) ?: default

  private fun getBitmapFromResId(
    context: Context,
    sourcePackageName: String,
    resourceId: Int,
  ): Bitmap? {
    val bitmap =
      context.packageManager
        .getDrawable(sourcePackageName, resourceId, /* appInfo= */ null)
        ?.toBitmapOrNull()
    if (bitmap == null) {
      Log.w(TAG, "Unable to load resourceId $resourceId from package $sourcePackageName")
    }
    return bitmap
  }

  /**
   * Get the value of this resource as a Drawable.
   *
   * Null will be returned if no Drawable can be created (e.g. if the resource ID is invalid, there
   * are permission problems, etc.)
   */
  fun getDrawable(context: Context): Drawable? {
    if (resourceReference == null) {
      if (value == null) {
        Log.e(TAG, "No reference or Bitmap provided")
        return null
      }
      return value.value.toDrawable(context.resources)
    }
    return context.packageManager.getDrawable(
      resourceReference.first,
      resourceReference.second,
      /* appInfo= */ null,
    )
  }

  /**
   * Returns true if this BitmapResource refers to a resource.
   *
   * This will return false is a raw Bitmap is contained in this [BitmapResource].
   */
  fun isResourceReference(): Boolean {
    return value == null
  }

  override fun toPersistableBundle() =
    PersistableBundle().apply {
      if (value != null) putPersistable(KEY_VALUE, value)
      if (resourceReference != null) {
        putString(KEY_PACKAGE_NAME, resourceReference.first)
        putInt(KEY_RESOURCE_ID, resourceReference.second)
      }
    }

  companion object : PersistableDeserializer<BitmapResource> {
    private const val TAG = "BitmapResource"
    private const val KEY_VALUE = "value"
    private const val KEY_PACKAGE_NAME = "packageName"
    private const val KEY_RESOURCE_ID = "resourceId"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): BitmapResource =
      if (persistableBundle.containsKey(KEY_VALUE))
        BitmapResource(
          persistableBundle.getPersistable(KEY_VALUE, PersistableBitmap)?.value
            ?: error("Missing bitmap resource value")
        )
      else
        BitmapResource(
          persistableBundle.getString(KEY_PACKAGE_NAME) ?: error("Missing bitmap package name"),
          persistableBundle.getInt(KEY_RESOURCE_ID),
        )
  }
}
