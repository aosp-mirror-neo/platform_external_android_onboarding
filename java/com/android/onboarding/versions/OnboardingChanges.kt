package com.android.onboarding.versions

import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.versions.annotations.ChangeRadius
import com.android.onboarding.versions.annotations.ValidChangeId

/** Entry point to checking if processes support particular change IDs. */
@OptIn(InternalOnboardingApi::class)
interface OnboardingChanges {

  /** Get the compliance date of a component. Defaults to [ComplianceDate.EARLIEST]. */
  @Deprecated("Use ComponentRef variant.")
  fun getComplianceDate(component: String): ComplianceDate =
    getComplianceDate(ComponentRef(component))

  /** Get the compliance date of a component. Defaults to [ComplianceDate.EARLIEST]. */
  fun getComplianceDate(component: ComponentRef): ComplianceDate

  /** True if the current executing process supports the given change ID. */
  fun currentProcessSupportsChange(@ValidChangeId changeId: Long): Boolean

  /**
   * Throws an [UnsupportedOperationException][java.lang.UnsupportedOperationException] if the
   * current executing process does not support the given change ID.
   */
  fun requireCurrentProcessSupportsChange(@ValidChangeId changeId: Long)

  /**
   * True if the given component supports the given change ID.
   *
   * If the component has not had [loadSupportedChanges] called previously, then it will be called
   * now. It is recommended that [loadSupportedChanges] is called ahead-of-time for all relevant
   * components to avoid arbitrary delays.
   *
   * If the component does not exist or is not valid, then it is assumed to support all released
   * changes but no unreleased changes.
   */
  @Deprecated("Use ComponentRef variant.")
  fun componentSupportsChange(
    component: String,
    @ValidChangeId(
      allowedChangeRadii = [ChangeRadius.MULTI_COMPONENT],
      disallowedChangeRadiiError =
        "You cannot query changes which are SINGLE_COMPONENT across process boundaries.",
    )
    changeId: Long,
  ): Boolean = componentSupportsChange(ComponentRef(component), changeId)

  /**
   * True if the given component supports the given change ID.
   *
   * If the component has not had [loadSupportedChanges] called previously, then it will be called
   * now. It is recommended that [loadSupportedChanges] is called ahead-of-time for all relevant
   * components to avoid arbitrary delays.
   *
   * If the component does not exist or is not valid, then it is assumed to support all released
   * changes but no unreleased changes.
   */
  fun componentSupportsChange(
    component: ComponentRef,
    @ValidChangeId(
      allowedChangeRadii = [ChangeRadius.MULTI_COMPONENT],
      disallowedChangeRadiiError =
        "You cannot query changes which are SINGLE_COMPONENT across process boundaries.",
    )
    changeId: Long,
  ): Boolean

  /**
   * Load the supported changes for the given component.
   *
   * This call involves IPC, and should ideally be performed in the background ahead of time.
   */
  @Deprecated("Use ComponentRef variant.")
  fun loadSupportedChanges(component: String) = loadSupportedChanges(ComponentRef(component))

  /**
   * Load the supported changes for the given component.
   *
   * This call involves IPC, and should ideally be performed in the background ahead of time.
   */
  fun loadSupportedChanges(component: ComponentRef)
}
