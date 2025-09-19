package com.android.onboarding.versions

import android.content.Context
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.utils.logging.OnboardingLog as Log

private const val LOG_TAG = "DefaultOnboardingChanges"

/**
 * Version of [DefaultOnboardingChanges] which supports changing versioning information.
 *
 * This can only be used by components which have the capability of manipulating global settings. It
 * should generally only be used by the single component in Onboarding which controls versioning for
 * the entire flow.
 */
@InternalOnboardingApi
@RequiresApi(24)
class DefaultMutableOnboardingChanges
@JvmOverloads
constructor(
  context: Context,
  private val onboardingChanges: DefaultOnboardingChanges = DefaultOnboardingChanges(context),
) : MutableOnboardingChanges, OnboardingChanges by onboardingChanges {

  private val context = context.applicationContext

  override fun setComplianceDate(component: String, complianceDate: ComplianceDate) {
    val packageName = onboardingChanges.extractPackageFromComponent(component)
    Log.d(TAG, "Setting compliance date for package $packageName $complianceDate")
    Settings.Global.putString(
      context.contentResolver,
      "$packageName.complianceDate",
      complianceDate.toFormattedString(),
    )
    loadSupportedChanges(packageName)
  }

  override fun enableTag(tag: String) {
    enableTag(context.packageName, tag)
  }

  override fun disableTag(tag: String) {
    disableTag(context.packageName, tag)
  }

  override fun setTagEnabled(component: String, tag: String, enabled: Boolean) {
    val packageName = onboardingChanges.extractPackageFromComponent(component)
    Log.d(TAG, "Setting tag enabled for package $packageName $tag=$enabled")
    Settings.Global.putInt(context.contentResolver, "$packageName.$tag", if (enabled) 1 else 0)
  }

  override fun enableChange(changeId: Long) {
    enableChange(context.packageName, changeId)
  }

  override fun disableChange(changeId: Long) {
    disableChange(context.packageName, changeId)
  }

  override fun setChangeEnabled(changeId: Long, enabled: Boolean) =
    setChangeEnabled(context.packageName, changeId, enabled)

  override fun setChangeEnabled(component: String, changeId: Long, enabled: Boolean) {
    val packageName = onboardingChanges.extractPackageFromComponent(component)
    Log.d(TAG, "Setting change enabled for package $packageName $changeId=$enabled")
    Settings.Global.putInt(context.contentResolver, "$packageName.$changeId", if (enabled) 1 else 0)
  }

  override fun resetChange(changeId: Long) {
    resetChange(context.packageName, changeId)
  }

  override fun resetChange(component: String, changeId: Long) {
    val packageName = onboardingChanges.extractPackageFromComponent(component)
    Log.d(TAG, "Resetting change enabled for package $packageName $changeId")
    Settings.Global.putString(context.contentResolver, "$packageName.$changeId", null)
  }

  override fun resetTag(tag: String) {
    resetTag(context.packageName, tag)
  }

  override fun resetTag(component: String, tag: String) {
    val packageName = onboardingChanges.extractPackageFromComponent(component)
    Log.d(TAG, "Resetting tag enabled for package $packageName $tag")
    Settings.Global.putString(context.contentResolver, "$packageName.$tag", null)
  }

  companion object {
    private const val TAG = "DefaultMutableOnboardingChanges"
  }
}

/** The meta-data key used to indicate compliance date. */
@InternalOnboardingApi const val COMPLIANCE_DATE_METADATA_KEY = "onboarding-compliance-date"
