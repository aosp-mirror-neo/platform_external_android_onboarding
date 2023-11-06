package com.android.onboarding.contracts.setupwizard

import com.android.onboarding.contracts.IntentScope
import com.android.onboarding.contracts.ScopedIntentSerializer
import com.google.android.setupcompat.util.WizardManagerHelper
import javax.inject.Inject

class SuwArgumentsSerializer @Inject constructor() : ScopedIntentSerializer<SuwArguments> {
  override fun IntentScope.write(value: SuwArguments) {
    intent["actionId"] = value.actionId
    intent["isSubactivityFirstLaunched"] = value.isSubactivityFirstLaunched
    intent[WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW] = value.isSuwSuggestedActionFlow
    intent[WizardManagerHelper.EXTRA_IS_SETUP_FLOW] = value.isSetupFlow
    intent[WizardManagerHelper.EXTRA_IS_PRE_DEFERRED_SETUP] = value.preDeferredSetup
    intent[WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP] = value.deferredSetup
    intent[WizardManagerHelper.EXTRA_IS_FIRST_RUN] = value.firstRun
    intent[WizardManagerHelper.EXTRA_IS_PORTAL_SETUP] = value.portalSetup
    intent["scriptUri"] = value.scriptUri
    intent["hasMultipleUsers"] = value.hasMultipleUsers
    intent[WizardManagerHelper.EXTRA_THEME] = value.theme
    intent["wizardBundle"] = value.wizardBundle
  }

  override fun IntentScope.read(): SuwArguments =
    SuwArguments(
      actionId = intExtraOrNull("actionId"),
      isSubactivityFirstLaunched = booleanExtraOrNull("isSubactivityFirstLaunched"),
      isSuwSuggestedActionFlow = booleanExtraOrNull("isSuwSuggestedActionFlow"),
      isSetupFlow = booleanExtraOrNull("isSetupFlow") ?: false,
      preDeferredSetup = booleanExtraOrNull("preDeferredSetup"),
      deferredSetup = booleanExtraOrNull("deferredSetup"),
      firstRun = booleanExtraOrNull("firstRun"),
      portalSetup = booleanExtraOrNull("portalSetup"),
      scriptUri = parcelableExtraOrNull("scriptUri"),
      hasMultipleUsers = booleanExtraOrNull("hasMultipleUsers"),
      theme = stringExtraOrNull("theme"),
      wizardBundle = bundleExtraOrNull("wizardBundle"),
    )
}
