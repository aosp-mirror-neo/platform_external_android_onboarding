package com.android.onboarding.utils.resources

import android.content.Context
import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer

/** A [String] which can either be provided directly or reference a string resource. */
data class StringResource
internal constructor(
  // Because of the constructors one of these will be non-null
  private val value: String?,
  // This is a pair of packageName, resourceId
  @get:VisibleForTesting val resourceReference: Pair<String, Int>?,
) : Persistable, StableEquality {

  constructor(string: String) : this(value = string, null)

  constructor(
    context: Context,
    resourceId: Int,
  ) : this(
    value =
      if (packagesWhoseTranslatedStringsCannotBeAccessedCrossApp.contains(context.packageName))
        context.getString(resourceId)
      else null,
    resourceReference = context.packageName to resourceId,
  )

  private constructor(
    packageName: String,
    resourceId: Int,
  ) : this(value = null, resourceReference = packageName to resourceId)

  private var hasCached: Boolean = false
  private var cachedValue: String? = null

  override fun toString(): String {
    if (value != null) {
      return "StringResource{value=$value}"
    }
    return "StringResource{package=${resourceReference!!.first}, resourceId=${resourceReference.second}}"
  }

  /**
   * Resolves the [String] represented by this resource.
   *
   * If the resource is not available or is not convertible to a [String] then null will be
   * returned.
   */
  fun get(context: Context): String? {
    if (!hasCached) {
      cachedValue =
        value ?: getStringFromResId(context, resourceReference!!.first, resourceReference.second)
      hasCached = true
    }

    if (value != null && resourceReference != null) {
      // Log to see if they resolve to the same value. If translated strings resolve to the same
      // value, we should remove the app from exercising the hack.
      val valueFromResId =
        getStringFromResId(context, resourceReference.first, resourceReference.second)
      Log.d(
        TAG,
        "Both value and reference are set by ${resourceReference.first}. rawValue=$value resolvedValue=$valueFromResId",
      )
    }

    return cachedValue
  }

  /**
   * Resolves the [String] represented by this resource.
   *
   * If the resource is not available or is not convertible to a [String] then [default] will be
   * returned.
   */
  fun get(context: Context, default: String): String = get(context) ?: default

  private fun getStringFromResId(
    context: Context,
    sourcePackageName: String,
    textResId: Int,
  ): String? {
    val string =
      context.packageManager.getText(sourcePackageName, textResId, /* appInfo= */ null)?.toString()
    if (string == null) {
      Log.w(TAG, "Unable to load resourceId $textResId from package $sourcePackageName")
    }
    return string
  }

  override fun toPersistableBundle() =
    PersistableBundle().apply {
      if (value != null) putString(KEY_VALUE, value)
      if (resourceReference != null) {
        putString(KEY_PACKAGE_NAME, resourceReference.first)
        putInt(KEY_RESOURCE_ID, resourceReference.second)
      }
    }

  companion object : PersistableDeserializer<StringResource> {
    private const val TAG = "StringResource"
    private const val KEY_VALUE = "value"
    private const val KEY_PACKAGE_NAME = "packageName"
    private const val KEY_RESOURCE_ID = "resourceId"

    /**
     * Some apps' localized strings can't be accessed by other apps by resources id during
     * SetupWizard. See b/394944798.
     */
    private val packagesWhoseTranslatedStringsCannotBeAccessedCrossApp =
      setOf(
        "com.google.android.gms" // GmsCore
      )

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): StringResource =
      if (persistableBundle.containsKey(KEY_VALUE))
        StringResource(
          persistableBundle.getString(KEY_VALUE) ?: error("Missing string resource value")
        )
      else
        StringResource(
          persistableBundle.getString(KEY_PACKAGE_NAME) ?: error("Missing string package name"),
          persistableBundle.getInt(KEY_RESOURCE_ID),
        )
  }
}
