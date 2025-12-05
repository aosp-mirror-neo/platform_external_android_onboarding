package com.android.onboarding.versions

import com.android.onboarding.contracts.annotations.InternalOnboardingApi
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
  protected val packageComplianceDates = mutableMapOf<String, ComplianceDate?>()

  /** Log a warn [message] with an optional [tr] error. */
  protected abstract fun warn(message: String, tr: Throwable?)

  /** Expand given [changeId] to a full [ChangeId] data. */
  protected abstract fun getChangeIdData(@ValidChangeId changeId: Long): ChangeId

  /** Is given [packageName] the same as current application package. */
  protected abstract fun isCurrentPackage(packageName: String): Boolean

  /** Get a string `<meta-data \>` value from application's manifest. */
  protected abstract fun getPackageMetaData(packageName: String, key: String): String?

  /** Get [String] value from global settings under [key]. */
  protected abstract fun getSettingString(key: String): String?

  /** Get [Int] value from global settings under [key]. */
  protected abstract fun getSettingInt(key: String): Int?

  /** Get value from system properties under [key]. */
  protected abstract fun getSystemProperty(key: String): String?

  final override fun getComplianceDate(component: String): ComplianceDate {
    val packageName = extractPackageFromComponent(component)
    val complianceOverride = getComplianceVersionOverride(packageName)
    return if (complianceOverride != null) {
      complianceOverride
    } else {
      val complianceDateString = getPackageMetaData(packageName, COMPLIANCE_DATE_METADATA_KEY)
      try {
        complianceDateString?.let(::ComplianceDate)
      } catch (e: DateTimeParseException) {
        warn("Invalid compliance date $complianceDateString", e)
        null
      } ?: ComplianceDate()
    }
  }

  final override fun componentSupportsChange(
    component: String,
    @ValidChangeId(
      allowedChangeRadii = [ChangeRadius.MULTI_COMPONENT],
      disallowedChangeRadiiError =
        "You cannot query changes which are SINGLE_COMPONENT across process boundaries.",
    )
    changeId: Long,
  ): Boolean {
    val packageName = extractPackageFromComponent(component)

    val changeData = getChangeIdData(changeId)

    if (
      changeData.changeRadius == ChangeRadius.SINGLE_COMPONENT && !isCurrentPackage(packageName)
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

    // If we can't fetch the supported features we assume no non-released features
    return packageComplianceDates[packageName]?.isAvailable(changeData) ?: false
  }

  final override fun loadSupportedChanges(component: String) {
    val packageName = extractPackageFromComponent(component)

    val complianceOverride = getComplianceVersionOverride(packageName)
    if (complianceOverride != null) {
      packageComplianceDates[packageName] = complianceOverride
      return
    }

    try {
      val complianceDateString =
        getPackageMetaData(packageName, COMPLIANCE_DATE_METADATA_KEY)
          ?: ComplianceDate.EARLIEST_COMPLIANCE_DATE_STRING
      packageComplianceDates[packageName] = ComplianceDate(complianceDateString)
    } catch (e: Exception) {
      warn("Could not find package info for $packageName", e)
      packageComplianceDates[packageName] = null
    }
  }

  private fun getComplianceVersionOverride(packageName: String): ComplianceDate? {
    val key = "$packageName.complianceDate"

    val dateString =
      getSettingString(key)
        ?: getSystemProperty(key)
        ?: getSystemProperty("persist.$key")
        ?: return null

    return ComplianceDate(dateString)
  }

  private fun getChangeOverride(packageName: String, changeData: ChangeId): Boolean? {
    val bugIdOverride = getSettingOrPropertyBoolean("$packageName.${changeData.bugId}")
    if (bugIdOverride != null) {
      return bugIdOverride
    }

    val tagOverrides =
      (changeData.tags + CHANGE_TAG_ALL).map { getSettingOrPropertyBoolean("$packageName.$it") }
    if (tagOverrides.contains(true)) {
      return true
    }

    if (tagOverrides.contains(false)) {
      return false
    }

    return null
  }

  private fun getSettingBoolean(key: String): Boolean? {
    val intValue =
      runCatching { getSettingInt(key) }
        .recoverCatching { getSettingInt("${key}L") }
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

  private fun getPropertyBoolean(key: String): Boolean? {
    val property =
      getSystemProperty(key)
        ?: getSystemProperty("${key}L")
        ?: getSystemProperty("persist.$key")
        ?: getSystemProperty("persist.${key}L")
        ?: return null
    return property == "1" ||
      property.equals("true", ignoreCase = true) ||
      property.equals("on", ignoreCase = true)
  }

  private fun getSettingOrPropertyBoolean(key: String): Boolean? =
    getSettingBoolean(key) ?: getPropertyBoolean(key)

  companion object {
    /**
     * Implicit [ChangeId.tags] value that is assumed for all change IDs. Enabling this tag will
     * enable all change IDs for a given package.
     */
    private const val CHANGE_TAG_ALL = "all"

    /** The meta-data key used to indicate compliance date. */
    @InternalOnboardingApi const val COMPLIANCE_DATE_METADATA_KEY = "onboarding-compliance-date"

    @InternalOnboardingApi
    fun extractPackageFromComponent(component: String) = component.split("/", limit = 2)[0]
  }
}
