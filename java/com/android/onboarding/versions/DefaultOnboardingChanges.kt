package com.android.onboarding.versions

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import android.provider.Settings
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.utils.logging.OnboardingLog
import com.android.onboarding.versions.annotations.ChangeId
import com.android.onboarding.versions.annotations.ValidChangeId
import com.android.onboarding.versions.changes.ALL_CHANGE_IDS

/**
 * Default implementation of [OnboardingChanges].
 *
 * Compatibility will be cached either when calling [loadSupportedChanges] or the first time a
 * change ID is checked where the component is not already cached. The cache can be reloaded by
 * calling [loadSupportedChanges] again.
 *
 * Versioning works by each onboarding component including a metadata of the following form in their
 * manifest:
 * ```xml
 * <meta-data android:name="onboarding-compliance-date" android:value="2023-07-10" />
 * ```
 *
 * Alternatively, apps with multiple subcomponent can specify compliance dates per-subcomponent by
 * defining non-translatable string resource, which when present will take precedence over app-level
 * compliance date for that subcomponent. The resource key must be formatted as
 * `<res_compliant_lowercase_subname>_onboarding_compliance_date`, where `.` and `-` are replaced
 * with `_`:
 * ```xml
 * <?xml version="1.0" encoding="utf-8"?>
 * <!-- res/values/strings.xml -->
 * <resources>
 *     <!-- Android Onboarding compliance date -->
 *     <string name="subcomponent_onboarding_compliance_date" translatable="false">2026-01-01</string>
 * </resources>
 * ```
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
 *
 * @param component the [ComponentRef] of the "current" component owning this instance for implicit
 *   change checks.
 */
@OptIn(InternalOnboardingApi::class)
class DefaultOnboardingChanges
@JvmOverloads
constructor(context: Context, component: ComponentRef? = null) : BaseOnboardingChanges() {
  private val applicationContext = context.applicationContext

  /**
   * The [ComponentRef] of the "current" component owning this instance for implicit change checks.
   */
  val owner: ComponentRef =
    component
      ?: ComponentRef(
        "${applicationContext.packageName}/${applicationContext.packageName.substringAfterLast(".")}"
      )

  override fun warn(message: String, tr: Throwable?) {
    if (tr != null) log.w(message, tr) else log.w(message)
  }

  override fun getChangeIdData(changeId: Long): ChangeId {
    return checkNotNull(ALL_CHANGE_IDS[changeId]) {
      "Invalid change ID $changeId - this should have been blocked by the conformance checker"
    }
  }

  override fun isCurrentPackage(packageName: String): Boolean {
    return packageName == owner.nodePackage
  }

  override fun getComplianceDateFromMetaData(component: ComponentRef): String? {
    val subcomponentComplianceDate =
      try {
        val res =
          applicationContext.packageManager.getResourcesForApplication(component.nodePackage)
        @Suppress("DiscouragedApi") // No other ways to get the value xApp
        val id = res.getIdentifier(complianceDateResKey(component), "string", component.nodePackage)
        res.getString(id)
      } catch (e: NameNotFoundException) {
        log.e("Package ${component.nodePackage} not found in package manager", e)
        null
      } catch (_: Resources.NotFoundException) {
        null
      }
    if (subcomponentComplianceDate != null) return subcomponentComplianceDate

    return try {
      val info =
        applicationContext.packageManager.getApplicationInfo(
          component.nodePackage,
          PackageManager.GET_META_DATA,
        )
      info.metaData?.getString(COMPLIANCE_DATE_METADATA_KEY)
    } catch (e: NameNotFoundException) {
      log.e("Package ${component.nodePackage} not found in package manager", e)
      null
    }
  }

  override fun getSettingString(key: String): String? {
    return try {
      Settings.Global.getString(applicationContext.contentResolver, key)
    } catch (_: Settings.SettingNotFoundException) {
      null
    }
  }

  override fun getSettingInt(key: String): Int? {
    return try {
      Settings.Global.getInt(applicationContext.contentResolver, key)
    } catch (_: Settings.SettingNotFoundException) {
      null
    }
  }

  @Suppress(
    // We've already confirmed it's not cross-boundary.
    "ValidChangeId"
  )
  override fun currentProcessSupportsChange(@ValidChangeId changeId: Long): Boolean {
    return componentSupportsChange(owner, changeId)
  }

  override fun requireCurrentProcessSupportsChange(@ValidChangeId changeId: Long) {
    if (currentProcessSupportsChange(changeId)) return

    // currentProcessSupportsChange will throw if this is null
    val changeData = getChangeIdData(changeId)

    if (changeData.available == ChangeId.NOT_AVAILABLE) {
      throw UnsupportedOperationException(
        "This API is not available. The change ($changeId) is set to not available. See OnboardingChanges for options for enabling this change during test and development."
      )
    } else {
      throw UnsupportedOperationException(
        "This API is not available. The change ($changeId) is available as of ${changeData.available} but the compliance date of this package is ${getComplianceDate(owner)}"
      )
    }
  }

  companion object {
    private val log = OnboardingLog.logger("DefaultOnboardingChanges")
  }
}
