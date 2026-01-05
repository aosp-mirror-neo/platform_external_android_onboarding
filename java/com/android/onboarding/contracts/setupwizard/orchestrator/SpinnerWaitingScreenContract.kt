package com.android.onboarding.contracts.setupwizard.orchestrator

import android.content.ComponentName
import com.android.onboarding.common.SETUP_WIZARD
import com.android.onboarding.contracts.annotations.ForegroundOnboardingNode
import com.android.onboarding.contracts.annotations.OnboardingNode

/**
 * Orchestrator for SetupWizard.
 *
 * This screen is responsible for controlling the flow of screens throughout Setup Wizard. It
 * fulfils the requirements of an Onboarding Orchestrator and so is able to execute Orchestrated
 * Tasks.
 *
 * This screen shows the "unified waiting screen spinner" interface which presents a sequence of
 * tasks and breaks out when it encounters a screen which cannot be ran in the background.
 *
 * Unlike [SetupWizardOrchestratorContract], this screen will only show a spinner with the metadata
 * for an arbitrarily chosen executing task and display that metadata in the header and description
 * fields.
 */
@OnboardingNode(
  component = SETUP_WIZARD,
  name = "SpinnerWaitingScreen",
  foreground = ForegroundOnboardingNode(uiType = ForegroundOnboardingNode.UiType.LOADING),
)
object SpinnerWaitingScreenContract : SetupWizardOrchestratorContract() {
  override val activityComponentName =
    ComponentName(
      "com.google.android.setupwizard",
      "com.google.android.setupwizard.SpinnerWaitingScreenActivity",
    )
  override val intentAction = null
}
