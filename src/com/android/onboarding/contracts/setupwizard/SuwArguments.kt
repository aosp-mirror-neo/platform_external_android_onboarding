package com.android.onboarding.contracts.setupwizard

import android.net.Uri
import android.os.Bundle

/**
 * Marker interface to keep track of all our contract arguments that either directly or indirectly
 * depend on [SuwArguments]
 */
interface WithSuwArguments : WithOptionalSuwArguments {
  override val suwArguments: SuwArguments
}

/**
 * Marker interface to keep track of all our contract arguments that optionally either directly or
 * indirectly depend on [SuwArguments]
 */
interface WithOptionalSuwArguments {
  val suwArguments: SuwArguments?
}

/** A set of common arguments unused by provisioning but still passed around to other processes. */
data class SuwArguments(
  val actionId: Int?,
  val isSubactivityFirstLaunched: Boolean?,
  val isSuwSuggestedActionFlow: Boolean,
  val isSetupFlow: Boolean,
  val preDeferredSetup: Boolean,
  val deferredSetup: Boolean,
  val firstRun: Boolean,
  val portalSetup: Boolean,
  val scriptUri: Uri?,
  val hasMultipleUsers: Boolean?,
  val theme: String,
  val wizardBundle: Bundle,
)
