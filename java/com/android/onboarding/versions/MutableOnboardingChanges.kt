package com.android.onboarding.versions

import androidx.annotation.RequiresApi

/**
 * Mutable version of [OnboardingChanges].
 *
 * This allows for dynamically changing the values of
 */
@RequiresApi(24)
interface MutableOnboardingChanges : OnboardingChanges {

  /** Set the compliance date of a component. */
  fun setComplianceDate(component: String, complianceDate: ComplianceDate)

  /** Forces all changes with a tag to be enabled. */
  fun enableTag(tag: String)

  /** Forces all changes with a tag to be enabled. */
  fun enableTag(component: String, tag: String) {
    setTagEnabled(component, tag, true)
  }

  /** Forces all changes with a tag to be disabled. */
  fun disableTag(tag: String)

  /** Forces all changes with a tag to be disabled. */
  fun disableTag(component: String, tag: String) {
    setTagEnabled(component, tag, false)
  }

  /** Forces all changes with a tag to be enabled/disabled. */
  fun setTagEnabled(component: String, tag: String, enabled: Boolean)

  /** Forces a change to be enabled. */
  fun enableChange(changeId: Long)

  /** Forces a change to be disabled. */
  fun disableChange(changeId: Long)

  /** Forces a change to be enabled. */
  fun enableChange(component: String, changeId: Long) {
    setChangeEnabled(component, changeId, true)
  }

  /** Forces a change to be disabled. */
  fun disableChange(component: String, changeId: Long) {
    setChangeEnabled(component, changeId, false)
  }

  /** Forces a change to be enabled/disabled. */
  fun setChangeEnabled(changeId: Long, enabled: Boolean)

  /** Forces a change to be enabled/disabled. */
  fun setChangeEnabled(component: String, changeId: Long, enabled: Boolean)

  /** Resets a change's availability to whatever is correct for the compliance date. */
  fun resetChange(changeId: Long)

  /** Resets a change's availability to whatever is correct for the compliance date. */
  fun resetChange(component: String, changeId: Long)

  /** Resets a tag's availability to whatever is correct for the compliance date. */
  fun resetTag(tag: String)

  /** Resets a tag's availability to whatever is correct for the compliance date. */
  fun resetTag(component: String, tag: String)
}
