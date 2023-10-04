package com.android.onboarding.contracts.setupwizard

import com.android.onboarding.contracts.IntentScope
import com.android.onboarding.contracts.ScopedIntentSerializer
import com.google.android.setupcompat.util.WizardManagerHelper
import javax.inject.Inject

class SuwArgumentsSerializer @Inject constructor() : ScopedIntentSerializer<SuwArguments> {
  override fun IntentScope.write(value: SuwArguments) {
   this["actionId"] = value.actionId
   this["isSubactivityFirstLaunched"] = value.isSubactivityFirstLaunched
   this[WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW] = value.isSuwSuggestedActionFlow
   this[WizardManagerHelper.EXTRA_IS_SETUP_FLOW] = value.isSetupFlow
   this[WizardManagerHelper.EXTRA_IS_PRE_DEFERRED_SETUP] = value.preDeferredSetup
   this[WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP] = value.deferredSetup
   this[WizardManagerHelper.EXTRA_IS_FIRST_RUN] = value.firstRun
   this[WizardManagerHelper.EXTRA_IS_PORTAL_SETUP] = value.portalSetup
   this["scriptUri"] = value.scriptUri
   this["hasMultipleUsers"] = value.hasMultipleUsers
   this[WizardManagerHelper.EXTRA_THEME] = value.theme
   this["wizardBundle"] = value.wizardBundle
  }

  override fun IntentScope.read(): SuwArguments =
    SuwArguments(
      actionId = intExtraOrNull("actionId"),
      isSubactivityFirstLaunched = booleanExtraOrNull("isSubactivityFirstLaunched"),
      isSuwSuggestedActionFlow = booleanExtra("isSuwSuggestedActionFlow"),
      isSetupFlow = booleanExtra("isSetupFlow"),
      preDeferredSetup = booleanExtra("preDeferredSetup"),
      deferredSetup = booleanExtra("deferredSetup"),
      firstRun = booleanExtra("firstRun"),
      portalSetup = booleanExtra("portalSetup"),
      scriptUri = parcelableExtraOrNull("scriptUri"),
      hasMultipleUsers = booleanExtra("hasMultipleUsers"),
      theme = stringExtra("theme"),
      wizardBundle = bundleExtra("wizardBundle"),
    )
}
