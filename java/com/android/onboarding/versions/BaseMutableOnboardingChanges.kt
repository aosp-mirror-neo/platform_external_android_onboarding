package com.android.onboarding.versions

import androidx.annotation.RequiresApi
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.versions.BaseOnboardingChanges.Companion.extractPackageFromComponent

/**
 * Base [MutableOnboardingChanges] implementation which is not platform-specific.
 *
 * @see DefaultMutableOnboardingChanges
 */
@InternalOnboardingApi
@RequiresApi(24)
abstract class BaseMutableOnboardingChanges(private val onboardingChanges: BaseOnboardingChanges) :
  MutableOnboardingChanges, OnboardingChanges by onboardingChanges {

  /** Log a debug [message]. */
  protected abstract fun debug(message: String)

  /** Set [String] setting under [key] to [value]. */
  protected abstract fun setSettingString(key: String, value: String?)

  /** Set [Int] setting under [key] to [value]. */
  protected abstract fun setSettingInt(key: String, value: Int)

  final override fun setComplianceDate(component: String, complianceDate: ComplianceDate) {
    val packageName = extractPackageFromComponent(component)
    debug("Setting compliance date for package $packageName $complianceDate")
    setSettingString("$packageName.complianceDate", complianceDate.toFormattedString())
    loadSupportedChanges(packageName)
  }

  final override fun setTagEnabled(component: String, tag: String, enabled: Boolean) {
    val packageName = extractPackageFromComponent(component)
    debug("Setting tag enabled for package $packageName $tag=$enabled")
    setSettingInt("$packageName.$tag", if (enabled) 1 else 0)
  }

  final override fun setChangeEnabled(component: String, changeId: Long, enabled: Boolean) {
    val packageName = extractPackageFromComponent(component)
    debug("Setting change enabled for package $packageName $changeId=$enabled")
    setSettingInt("$packageName.$changeId", if (enabled) 1 else 0)
  }

  final override fun resetChange(component: String, changeId: Long) {
    val packageName = extractPackageFromComponent(component)
    debug("Resetting change enabled for package $packageName $changeId")
    setSettingString("$packageName.$changeId", null)
  }

  final override fun resetTag(component: String, tag: String) {
    val packageName = extractPackageFromComponent(component)
    debug("Resetting tag enabled for package $packageName $tag")
    setSettingString("$packageName.$tag", null)
  }
}
