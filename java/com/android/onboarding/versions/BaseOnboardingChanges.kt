package com.android.onboarding.versions

import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.versions.annotations.ChangeId
import com.android.onboarding.versions.annotations.ChangeRadius
import com.android.onboarding.versions.annotations.ValidChangeId
import java.time.format.DateTimeParseException

/**
 * Base [OnboardingChanges] implementation which is not platform-specific.
 *
 * @see DefaultOnboardingChanges
 */
@InternalOnboardingApi
abstract class BaseOnboardingChanges : OnboardingChanges {

  /**
   * Map of package to the compliance date for that package.
   *
   * If a package has failed to be loaded, the value will be null. If it has not been attempted the
   * key will be non-present.
   */
  private val packageComplianceDates = mutableMapOf<String, ComplianceDate?>()

  /** Log warn [message] with an optional [tr] error. */
  protected abstract fun warn(message: String, tr: Throwable?)

  /** Expand given [changeId] to a full [ChangeId] data. */
  protected abstract fun getChangeIdData(@ValidChangeId changeId: Long): ChangeId

  /** Is given [packageName] the same as current application package. */
  protected abstract fun isCurrentPackage(packageName: String): Boolean?

  /** Get a compliance date string from `<meta-data \>` value for given [component]. */
  protected abstract fun getComplianceDateFromMetaData(component: ComponentRef): String?

  /** Get [String] value from global settings under [key]. */
  protected abstract fun getSettingString(key: String): String?

  /** Get [Int] value from global settings under [key]. */
  protected abstract fun getSettingInt(key: String): Int?

  final override fun getComplianceDate(component: ComponentRef): ComplianceDate {
    return getComplianceDateOrNull(component) ?: ComplianceDate()
  }

  /** Get [ComplianceDate] or null if an error occurs or component does not specify it. */
  fun getComplianceDateOrNull(component: ComponentRef): ComplianceDate? {
    val packageName = component.nodePackage
    val complianceOverride = getComplianceVersionOverride(packageName)
    return if (complianceOverride != null) {
      complianceOverride
    } else {
      val complianceDateString = getComplianceDateFromMetaData(component)
      try {
        complianceDateString?.let(::ComplianceDate)
      } catch (e: DateTimeParseException) {
        warn("Invalid compliance date $complianceDateString", e)
        null
      }
    }
  }

  /**
   * Checks if the [changeId] is enabled for [component].
   *
   * Since this bypasses [ChangeRadius] checks, the method returning true does not necessarily mean
   * that it is safe for the calling component to use the feature guarded by [changeId].
   */
  @InternalOnboardingApi
  fun isChangeEnabledForComponent(component: ComponentRef, @ValidChangeId changeId: Long): Boolean {
    val packageName = component.nodePackage
    val changeData = getChangeIdData(changeId)
    return componentSupportsChange(packageName, changeData, component)
  }

  final override fun componentSupportsChange(
    component: ComponentRef,
    @ValidChangeId(
      allowedChangeRadii = [ChangeRadius.MULTI_COMPONENT],
      disallowedChangeRadiiError =
        "You cannot query changes which are SINGLE_COMPONENT across process boundaries.",
    )
    changeId: Long,
  ): Boolean {
    val packageName = component.nodePackage

    val changeData = getChangeIdData(changeId)

    if (
      changeData.changeRadius == ChangeRadius.SINGLE_COMPONENT &&
        isCurrentPackage(packageName) == false
    ) {
      // This should be blocked by conformance
      throw IllegalArgumentException(
        "You cannot query changes which are SINGLE_COMPONENT across process boundaries."
      )
    }

    return componentSupportsChange(packageName, changeData, component)
  }

  private fun componentSupportsChange(
    packageName: String,
    changeData: ChangeId,
    component: ComponentRef,
  ): Boolean {
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

    loadSupportedChanges(component)
    // }

    // If we can't fetch the supported features we assume no non-released features
    return packageComplianceDates[packageName]?.isAvailable(changeData) ?: false
  }

  final override fun loadSupportedChanges(component: ComponentRef) {
    val packageName = component.nodePackage

    val complianceOverride = getComplianceVersionOverride(packageName)
    if (complianceOverride != null) {
      packageComplianceDates[packageName] = complianceOverride
      return
    }

    try {
      val complianceDateString =
        getComplianceDateFromMetaData(component) ?: ComplianceDate.EARLIEST_COMPLIANCE_DATE_STRING
      packageComplianceDates[packageName] = ComplianceDate(complianceDateString)
    } catch (e: Exception) {
      warn("Could not find package info for $packageName", e)
      packageComplianceDates[packageName] = null
    }
  }

  private fun getComplianceVersionOverride(packageName: String): ComplianceDate? =
    getSettingString("$packageName.complianceDate")?.let(::ComplianceDate)

  private fun getChangeOverride(packageName: String, changeData: ChangeId): Boolean? {
    val bugIdOverride = getSettingBoolean("$packageName.${changeData.bugId}", tag = false)
    if (bugIdOverride != null) return bugIdOverride

    val tagOverride =
      (changeData.tags + CHANGE_TAG_ALL)
        .asSequence()
        .map { getSettingBoolean("$packageName.$it", tag = true) }
        .firstOrNull { it != null }
    return tagOverride
  }

  private fun getSettingBoolean(key: String, tag: Boolean): Boolean? {
    val intValue =
      runCatching { getSettingInt(key) }
        .recoverCatching { if (!tag) getSettingInt("${key}L") else null }
        .getOrNull()
        ?.equals(1)
    if (intValue != null) return intValue

    val strValue =
      runCatching { getSettingString(key) }
        .recoverCatching { getSettingString("${key}L") }
        .getOrNull()
    return if (strValue != null) {
      strValue == "1" ||
        strValue.equals("true", ignoreCase = true) ||
        strValue.equals("on", ignoreCase = true)
    } else {
      null
    }
  }

  companion object {
    /**
     * Implicit [ChangeId.tags] value that is assumed for all change IDs. Enabling this tag will
     * enable all change IDs for a given package.
     */
    private const val CHANGE_TAG_ALL = "all"

    /** The meta-data key used to indicate compliance date in android manifest meta-data. */
    @InternalOnboardingApi const val COMPLIANCE_DATE_METADATA_KEY = "onboarding-compliance-date"

    /** The meta-data key used to indicate compliance date in android resources. */
    @InternalOnboardingApi
    fun complianceDateResKey(component: ComponentRef): String {
      val resPrefix = component.subcomponent.replace(".", "_").replace("-", "_")
      return "${resPrefix}_onboarding_compliance_date"
    }
  }
}
