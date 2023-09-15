package com.android.onboarding.contracts.provisioning.suw

import android.net.Uri
import android.os.Bundle

/**
 * A set of common arguments unused by provisioning but still passed around to other processes.
 */
interface SuwArguments {
  val actionId: Int?
  val isSubactivityFirstLaunched: Boolean
  val isSuwSuggestedActionFlow: Boolean
  val isSetupFlow: Boolean
  val preDeferredSetup: Boolean
  val deferredSetup: Boolean
  val firstRun: Boolean
  val portalSetup: Boolean
  val scriptUri: Uri?
  val hasMultipleUsers: Boolean
  val theme: String
  val wizardBundle: Bundle
}
