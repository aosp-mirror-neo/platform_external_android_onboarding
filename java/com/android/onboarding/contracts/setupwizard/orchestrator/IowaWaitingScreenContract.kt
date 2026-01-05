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
 * This screen shows the "unified waiting screen" interface which presents a sequence of tasks and
 * breaks out when it encounters a screen which cannot be ran in the background.
 *
 * Currently, this is only used for the initial loading during the quickstart flow, and control is
 * passed back to [WizardManagerActivity] whenever an issue is encountered.
 */
@OnboardingNode(
  component = SETUP_WIZARD,
  name = "IowaWaitingScreen",
  foreground = ForegroundOnboardingNode(uiType = ForegroundOnboardingNode.UiType.LOADING),
)
object IowaWaitingScreenContract : SetupWizardOrchestratorContract() {
  override val activityComponentName =
    ComponentName(
      "com.google.android.setupwizard",
      "com.google.android.setupwizard.IowaWaitingScreenActivity",
    )
  override val intentAction = "com.android.wizard.ORCHESTRATOR"
}
