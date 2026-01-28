package com.android.onboarding.contracts.setupwizard.orchestrator

import android.os.PersistableBundle
import com.android.onboarding.common.SETUP_WIZARD
import com.android.onboarding.compatibility.compatPutBoolean
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.control.ActivityLaunch
import com.android.onboarding.tasks.OnboardingTaskContract
import com.android.onboarding.utils.persistable.PersistableBundle
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import com.android.onboarding.wizardmanager.actions.PersistableWizardResult
import com.google.android.wizardmanager.WizardManagerExtras

/**
 * Task that records the result of an action execution via the Orchestrator.
 *
 * This is the equivalent to launching [SetupWizardOrchestratorContract] but performed entirely in
 * the background.
 *
 * It will return immediately with an Activity to launch to continue the flow. This will be a
 * loading screen if there is nothing else to show.
 */
@OnboardingNode(component = SETUP_WIZARD, name = "RecordActionResult")
object RecordActionResultContract :
  OnboardingTaskContract<SetupWizardOrchestratorContract.OrchestratorArgument, ActivityLaunch>() {

  private const val KEY_EXTRAS = "extras"
  private const val KEY_RESULT = "result"
  private const val KEY_ACTION_ALREADY_EXECUTED = "actionAlreadyExecuted"
  private const val KEY_ACTIVITY_LAUNCH = "activityLaunch"

  override fun performEncodeArgs(taskArgs: SetupWizardOrchestratorContract.OrchestratorArgument) =
    PersistableBundle {
      putPersistable(KEY_EXTRAS, taskArgs.extras)
      compatPutBoolean(KEY_ACTION_ALREADY_EXECUTED, taskArgs.actionAlreadyExecuted)
      when (val action = taskArgs.action) {
        is SetupWizardOrchestratorContract.Action.ResultAction -> {
          putPersistable(KEY_RESULT, action.result)
        }
        is SetupWizardOrchestratorContract.Action.TaskAction -> {
          error("TaskAction not supported in RecordActionResult")
        }
      }
    }

  override fun performExtractArgs(
    bundle: PersistableBundle
  ): SetupWizardOrchestratorContract.OrchestratorArgument {
    val extras = bundle.getPersistable(KEY_EXTRAS, WizardManagerExtras) ?: error("Missing extras")
    val result =
      bundle.getPersistable(KEY_RESULT, PersistableWizardResult) ?: error("Missing result")
    val actionAlreadyExecuted = bundle.getBoolean(KEY_ACTION_ALREADY_EXECUTED, false)
    return SetupWizardOrchestratorContract.OrchestratorArgument(
      extras = extras,
      action = SetupWizardOrchestratorContract.Action.ResultAction(result),
      actionAlreadyExecuted = actionAlreadyExecuted,
    )
  }

  override fun performEncodeResult(taskResult: ActivityLaunch): PersistableBundle =
    PersistableBundle {
      putPersistable(KEY_ACTIVITY_LAUNCH, taskResult)
    }

  override fun performExtractResult(bundle: PersistableBundle): ActivityLaunch {
    return bundle.getPersistable(KEY_ACTIVITY_LAUNCH, ActivityLaunch)
      ?: error("Missing Activity Launch")
  }
}
