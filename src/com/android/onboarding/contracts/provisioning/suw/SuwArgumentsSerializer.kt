package com.android.onboarding.contracts.provisioning.suw

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.android.onboarding.contracts.IntentSerializer
import com.android.onboarding.contracts.provisioning.ArgumentSerializer

class SuwArgumentsSerializer : ArgumentSerializer(), IntentSerializer<SuwArguments> {

  override fun write(intent: Intent, value: SuwArguments): Unit =
    with(value) {
      intent["actionId"] = actionId
      intent["isSubactivityFirstLaunched"] = isSubactivityFirstLaunched
      intent["isSuwSuggestedActionFlow"] = isSuwSuggestedActionFlow
      intent["isSetupFlow"] = isSetupFlow
      intent["preDeferredSetup"] = preDeferredSetup
      intent["deferredSetup"] = deferredSetup
      intent["firstRun"] = firstRun
      intent["portalSetup"] = portalSetup
      intent["scriptUri"] = scriptUri
      intent["hasMultipleUsers"] = hasMultipleUsers
      intent["theme"] = theme
      intent["wizardBundle"] = wizardBundle
    }

  override fun read(intent: Intent): SuwArguments {
    return object : SuwArguments {
      override val actionId: Int? = intent.intExtraOrNull("actionId")

      override val isSubactivityFirstLaunched: Boolean =
        intent.booleanExtra("isSubactivityFirstLaunched")

      override val isSuwSuggestedActionFlow: Boolean =
        intent.booleanExtra("isSuwSuggestedActionFlow")

      override val isSetupFlow: Boolean = intent.booleanExtra("isSetupFlow")

      override val preDeferredSetup: Boolean = intent.booleanExtra("preDeferredSetup")

      override val deferredSetup: Boolean = intent.booleanExtra("deferredSetup")

      override val firstRun: Boolean = intent.booleanExtra("firstRun")

      override val portalSetup: Boolean = intent.booleanExtra("portalSetup")

      override val scriptUri: Uri? = intent.parcelableExtra("scriptUri")

      override val hasMultipleUsers: Boolean = intent.booleanExtra("hasMultipleUsers")

      override val theme: String = intent.stringExtra("theme")

      override val wizardBundle: Bundle = intent.bundleExtra("wizardBundle")
    }
  }
}
