package com.android.onboarding.versions

import androidx.annotation.RequiresApi
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.ComponentRef

/**
 * Mutable version of [OnboardingChanges].
 *
 * This allows for dynamically changing the values of Onboarding Changes.
 */
@OptIn(InternalOnboardingApi::class)
@RequiresApi(24)
interface MutableOnboardingChanges : OnboardingChanges {

  /** Set the compliance date of a component. */
  @Deprecated("Use ComponentRef variant.")
  fun setComplianceDate(component: String, complianceDate: ComplianceDate) =
    setComplianceDate(ComponentRef(component), complianceDate)

  /** Set the compliance date of a component. */
  fun setComplianceDate(component: ComponentRef, complianceDate: ComplianceDate)

  /** Forces all changes with a tag to be enabled. */
  fun enableTag(tag: String)

  /** Forces all changes with a tag to be enabled. */
  @Deprecated("Use ComponentRef variant.")
  fun enableTag(component: String, tag: String) = enableTag(ComponentRef(component), tag)

  /** Forces all changes with a tag to be enabled. */
  fun enableTag(component: ComponentRef, tag: String) {
    setTagEnabled(component, tag, true)
  }

  /** Forces all changes with a tag to be disabled. */
  fun disableTag(tag: String)

  /** Forces all changes with a tag to be disabled. */
  @Deprecated("Use ComponentRef variant.")
  fun disableTag(component: String, tag: String) = disableTag(ComponentRef(component), tag)

  /** Forces all changes with a tag to be disabled. */
  fun disableTag(component: ComponentRef, tag: String) {
    setTagEnabled(component, tag, false)
  }

  /** Forces all changes with a tag to be enabled/disabled. */
  @Deprecated("Use ComponentRef variant.")
  fun setTagEnabled(component: String, tag: String, enabled: Boolean) =
    setTagEnabled(ComponentRef(component), tag, enabled)

  /** Forces all changes with a tag to be enabled/disabled. */
  fun setTagEnabled(component: ComponentRef, tag: String, enabled: Boolean)

  /** Forces a change to be enabled. */
  fun enableChange(changeId: Long)

  /** Forces a change to be disabled. */
  fun disableChange(changeId: Long)

  /** Forces a change to be enabled. */
  @Deprecated("Use ComponentRef variant.")
  fun enableChange(component: String, changeId: Long) =
    enableChange(ComponentRef(component), changeId)

  /** Forces a change to be enabled. */
  fun enableChange(component: ComponentRef, changeId: Long) {
    setChangeEnabled(component, changeId, true)
  }

  /** Forces a change to be disabled. */
  @Deprecated("Use ComponentRef variant.")
  fun disableChange(component: String, changeId: Long) =
    disableChange(ComponentRef(component), changeId)

  /** Forces a change to be disabled. */
  fun disableChange(component: ComponentRef, changeId: Long) {
    setChangeEnabled(component, changeId, false)
  }

  /** Forces a change to be enabled/disabled. */
  fun setChangeEnabled(changeId: Long, enabled: Boolean)

  /** Forces a change to be enabled/disabled. */
  @Deprecated("Use ComponentRef variant.")
  fun setChangeEnabled(component: String, changeId: Long, enabled: Boolean) =
    setChangeEnabled(ComponentRef(component), changeId, enabled)

  /** Forces a change to be enabled/disabled. */
  fun setChangeEnabled(component: ComponentRef, changeId: Long, enabled: Boolean)

  /** Resets a change's availability to whatever is correct for the compliance date. */
  fun resetChange(changeId: Long)

  /** Resets a change's availability to whatever is correct for the compliance date. */
  @Deprecated("Use ComponentRef variant.")
  fun resetChange(component: String, changeId: Long) =
    resetChange(ComponentRef(component), changeId)

  /** Resets a change's availability to whatever is correct for the compliance date. */
  fun resetChange(component: ComponentRef, changeId: Long)

  /** Resets a tag's availability to whatever is correct for the compliance date. */
  fun resetTag(tag: String)

  /** Resets a tag's availability to whatever is correct for the compliance date. */
  @Deprecated("Use ComponentRef variant.")
  fun resetTag(component: String, tag: String) = resetTag(ComponentRef(component), tag)

  /** Resets a tag's availability to whatever is correct for the compliance date. */
  fun resetTag(component: ComponentRef, tag: String)
}
