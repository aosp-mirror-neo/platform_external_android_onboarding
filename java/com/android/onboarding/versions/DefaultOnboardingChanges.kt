package com.android.onboarding.versions

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.SystemProperties
import android.provider.Settings
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.utils.logging.OnboardingLog
import com.android.onboarding.versions.annotations.ChangeId
import com.android.onboarding.versions.annotations.ChangeRadius
import com.android.onboarding.versions.annotations.ValidChangeId
import com.android.onboarding.versions.changes.ALL_CHANGE_IDS
import java.time.format.DateTimeParseException

private const val LOG_TAG = "DefaultOnboardingChanges"

/**
 * Default implementation of [OnboardingChanges].
 *
 * Compatibility will be cached either when calling [loadSupportedChanges] or the first time a
 * change ID is checked where the component is not already cached. The cache can be reloaded by
 * calling [loadSupportedChanges] again.
 *
 * Versioning works by each onboarding component including a meta data of the following form in
 * their manifest:
 *
 * <meta-data android:name="onboarding-compliance-date" android:value="2023-07-10" />
 *
 * This indicates that this APK is compatible with onboarding APIs as of 2023-07-10. Breaking API
 * changes should not be introduced from the point of view of this APK without that compliance date
 * being increased.
 *
 * Note that any single onboarding session will be made up of several APKs working to different
 * compliance dates, so APKs should not assume that everyone is using the same APIs.
 *
 * For local testing, use `adb shell settings put global package.name.changeid 1` to enable and `adb
 * shell settings put global package.name.changeid 0` to disable. For example to enable change ID
 * 1234 on package `com.android.setup` we would use `adb shell settings put global
 * com.android.setup.1234 1`. You can also use `adb shell settings delete global
 * package.name.changeid` to use the default configured value.
 *
 * Alternatively you can use a tag name instead of a change id.
 *
 * We can also instead override the compliance version of a package using `adb shell settings put
 * global package.name.complianceDate 2020-10-01` and `adb shell settings delete
 * package.name.complianceDate` to reset. You will need to clear the cache for this change to take
 * effect.
 *
 * On SDK 26+, System properties can also be used as an alternative to global settings. If both a
 * global setting and a system property are set, the global setting will take priority.
 */
@OptIn(InternalOnboardingApi::class)
class DefaultOnboardingChanges(context: Context) : OnboardingChanges {

  private val context = context.applicationContext

  /**
   * Map of package to the compliance date for that package.
   *
   * If a package has failed to be loaded, the value will be null. If it has not been attempted the
   * key will be non-present.
   */
  private val packageComplianceDates = mutableMapOf<String, ComplianceDate?>()

  override fun getComplianceDate(component: String): ComplianceDate {
    val packageName = extractPackageFromComponent(component)
    val complianceOverride = getComplianceVersionOverride(context, packageName)
    return if (complianceOverride != null) {
      complianceOverride
    } else {
      val complianceDateString =
        try {
          val app: ApplicationInfo =
            context.packageManager.getApplicationInfo(
              context.packageName,
              PackageManager.GET_META_DATA,
            )
          app.metaData?.getString(COMPLIANCE_DATE_METADATA_KEY)
        } catch (e: NameNotFoundException) {
          OnboardingLog.e(TAG, "Package ${context.packageName} not found in package manager")
          null
        }

      try {
        complianceDateString?.let(::ComplianceDate)
      } catch (e: DateTimeParseException) {
        OnboardingLog.i(LOG_TAG, "Invalid compliance date $complianceDateString", e)
        null
      } ?: ComplianceDate()
    }
  }

  private fun getComplianceVersionOverride(context: Context, packageName: String): ComplianceDate? {
    val settingKey = "$packageName.complianceDate"

    val dateString =
      Settings.Global.getString(context.contentResolver, settingKey)
        ?: if (Build.VERSION.SDK_INT >= 26) SystemProperties.get(settingKey) else ""
    // On less than 26 we had length limits for system properties - we'll just not support this
    // feature on older versions.

    if (dateString.isEmpty()) return null

    return ComplianceDate(dateString)
  }

  @Suppress(
    // We've already confirmed it's not cross-boundary.
    "ValidChangeId"
  )
  override fun currentProcessSupportsChange(@ValidChangeId changeId: Long): Boolean {
    return componentSupportsChange(context.packageName, changeId)
  }

  override fun requireCurrentProcessSupportsChange(@ValidChangeId changeId: Long) {
    if (currentProcessSupportsChange(changeId)) {
      return
    }

    // currentProcessSupportsChange will throw if this is null
    val changeData = ALL_CHANGE_IDS[changeId]!!

    if (changeData.available == ChangeId.NOT_AVAILABLE) {
      throw UnsupportedOperationException(
        "This API is not available. The change ($changeId) is set to not available. See OnboardingChanges for options for enabling this change during test and development."
      )
    }

    throw UnsupportedOperationException(
      "This API is not available. The change ($changeId) is available as of ${changeData.available} but the compliance date of this package is ${getComplianceDate(context.packageName)}"
    )
  }

  override fun loadSupportedChanges(component: String) {
    val packageName = extractPackageFromComponent(component)

    val complianceOverride = getComplianceVersionOverride(context, packageName)
    if (complianceOverride != null) {
      packageComplianceDates[packageName] = complianceOverride
      return
    }

    try {
      val packageInfo =
        context.packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)

      val complianceDateString =
        packageInfo.applicationInfo?.metaData?.getString("onboarding-compliance-date")
          ?: ComplianceDate.EARLIEST_COMPLIANCE_DATE_STRING
      packageComplianceDates[packageName] = ComplianceDate(complianceDateString)
    } catch (e: NameNotFoundException) {
      OnboardingLog.w(LOG_TAG, "Could not find package info for $packageName", e)
      packageComplianceDates[packageName] = null
    }
  }

  override fun componentSupportsChange(
    component: String,
    @ValidChangeId(
      allowedChangeRadii = [ChangeRadius.MULTI_COMPONENT],
      disallowedChangeRadiiError =
        "You cannot query changes which are SINGLE_COMPONENT across process boundaries.",
    )
    changeId: Long,
  ): Boolean {
    val packageName = extractPackageFromComponent(component)

    val changeData =
      ALL_CHANGE_IDS[changeId]
        ?: throw IllegalStateException(
          "Invalid change ID $changeId - this should have been blocked by the conformance checker"
        )

    if (
      changeData.changeRadius == ChangeRadius.SINGLE_COMPONENT && packageName != context.packageName
    ) {
      // This should be blocked by conformance
      throw IllegalArgumentException(
        "You cannot query changes which are SINGLE_COMPONENT across process boundaries."
      )
    }

    val changeOverride = getChangeOverride(packageName, changeData)
    if (changeOverride != null) {
      return changeOverride
    }

    // If it's released, we assume it's available everywhere
    // (this call should be blocked by conformance)
    if (changeData.released != ChangeId.NOT_RELEASED) {
      return true
    }

    // if (!packageComplianceDates.containsKey(packageName)) {

    loadSupportedChanges(packageName)
    // }

    val p = packageComplianceDates[packageName]?.isAvailable(changeData) ?: false

    // If we can't fetch the supported features we assume no non-released features
    return p
  }

  private fun getChangeOverride(packageName: String, changeData: ChangeId): Boolean? {
    val bugIdOverride = getSettingOrPropertyBoolean(context, "$packageName.${changeData.bugId}")
    if (bugIdOverride != null) {
      return bugIdOverride
    }

    val tagOverrides =
      changeData.tags.map { getSettingOrPropertyBoolean(context, "$packageName.$it") }
    if (tagOverrides.contains(true)) {
      return true
    }

    if (tagOverrides.contains(false)) {
      return false
    }

    return null
  }

  private fun getSettingOrPropertyBoolean(context: Context, key: String): Boolean? {
    val setting =
      try {
        Settings.Global.getInt(context.contentResolver, key) == 1
      } catch (_: Settings.SettingNotFoundException) {
        null
      }
        ?: try {
          Settings.Global.getInt(context.contentResolver, "${key}L") == 1
        } catch (_: Settings.SettingNotFoundException) {
          null
        }

    if (setting != null) return setting

    val property =
      if (Build.VERSION.SDK_INT >= 26) {
        SystemProperties.get(key).takeIf(String::isNotBlank) ?: SystemProperties.get("${key}L")
      } else {
        // On less than 26 we had length limits for system properties - we'll just not support this
        // feature on older versions.
        ""
      }

    if (property.isBlank()) return null

    return property == "1"
  }

  @InternalOnboardingApi
  fun extractPackageFromComponent(component: String) = component.split("/", limit = 2)[0]

  companion object {
    private const val TAG = "DefaultOnboardingChanges"
  }
}
