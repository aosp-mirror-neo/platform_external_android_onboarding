package com.android.onboarding.contracts.setupwizard

import com.android.onboarding.contracts.IntentScope
import com.android.onboarding.contracts.ScopedIntentSerializer
import com.google.android.setupcompat.util.WizardManagerHelper
import javax.inject.Inject

class SuwArgumentsSerializer @Inject constructor() : ScopedIntentSerializer<SuwArguments> {
  override fun IntentScope.write(value: SuwArguments) {
    intent[WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW] = value.isSuwSuggestedActionFlow
    intent[WizardManagerHelper.EXTRA_IS_SETUP_FLOW] = value.isSetupFlow
    intent[WizardManagerHelper.EXTRA_IS_PRE_DEFERRED_SETUP] = value.preDeferredSetup
    intent[WizardManagerHelper.EXTRA_IS_DEFERRED_SETUP] = value.deferredSetup
    intent[WizardManagerHelper.EXTRA_IS_FIRST_RUN] = value.firstRun
    intent[WizardManagerHelper.EXTRA_IS_PORTAL_SETUP] = value.portalSetup
    intent["wizardBundle"] = value.wizardBundle
    intent[WizardManagerHelper.EXTRA_THEME] = value.theme
    intent["hasMultipleUsers"] = value.hasMultipleUsers
    intent["isSubactivityFirstLaunched"] = value.isSubactivityFirstLaunched
  }

  override fun IntentScope.read(): SuwArguments =
    SuwArguments(
      isSuwSuggestedActionFlow = booleanExtra("isSuwSuggestedActionFlow"),
      isSetupFlow = booleanExtra("isSetupFlow"),
      preDeferredSetup = booleanExtra("preDeferredSetup"),
      deferredSetup = booleanExtra("deferredSetup"),
      firstRun = booleanExtra("firstRun"),
      portalSetup = booleanExtra("portalSetup"),
      wizardBundle = bundleExtra("wizardBundle"),
      theme = stringExtra("theme"),
      hasMultipleUsers = booleanExtraOrNull("hasMultipleUsers"),
      isSubactivityFirstLaunched = booleanExtraOrNull("isSubactivityFirstLaunched"),
    )
}
