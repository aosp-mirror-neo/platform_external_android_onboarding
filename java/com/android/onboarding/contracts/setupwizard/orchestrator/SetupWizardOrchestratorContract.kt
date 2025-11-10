package com.android.onboarding.contracts.setupwizard.orchestrator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import com.android.onboarding.common.SETUP_WIZARD
import com.android.onboarding.contracts.VoidOnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.ForegroundOnboardingNode
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistableExtra
import com.android.onboarding.wizardmanager.actions.PersistableWizardResult
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.android.wizardmanager.WizardManagerExtras

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
  name = "SetupWizardOrchestrator",
  foreground = ForegroundOnboardingNode(uiType = ForegroundOnboardingNode.UiType.LOADING),
)
object SetupWizardOrchestratorContract :
  VoidOnboardingActivityApiContract<SetupWizardOrchestratorContract.OrchestratorArgument>() {

  /**
   * Argument used for entering orchestration.
   *
   * @param extras WizardManagerExtras which must have been initialised earlier in the flow.
   * @param action The action to be performed by the loading screen
   */
  data class OrchestratorArgument(val extras: WizardManagerExtras, val action: Action)

  sealed class Action {
    /** [Action] which indicates the given result should be used to continue the onboarding flow. */
    data class ResultAction(val result: PersistableWizardResult) : Action()

    /**
     * [Action] which indicates that the given task should be executed, and its result used to
     * continue the onboarding flow.
     *
     * If the orchestration infrastructure fails, then it will be disabled and the [onFailure]
     * contract launched. This should allow continuing the flow without using orchestration.
     */
    @Deprecated("Not supported - NextActionController is entirely responsible for task execution")
    data class TaskAction(
      val task: PendingContract.ErasedTask,
      val onFailure: PendingContract.ErasedActivity?,
    ) : Action()
  }

  override fun performCreateIntent(context: Context, arg: OrchestratorArgument): Intent =
    Intent(INTENT_ACTION).also {
      it.setComponent(
        ComponentName(
          "com.google.android.setupwizard",
          "com.google.android.setupwizard.SetupWizardOrchestratorActivity",
        )
      )
      it.addCategory(CATEGORY_DEFAULT)
      it.putPersistableExtra(KEY_EXTRAS, arg.extras)

      /**
       * Copy all [WizardManagerExtras] directly to the resultant intent, not just inside
       * [KEY_EXTRAS].
       *
       * This allows standard SetupWizard infrastructure tools, e.g. [WizardManagerCompatHelper], to
       * be used within [SetupWizardOrchestratorActivity].
       */
      val srcIntentContainerForWizardManagerExtras =
        Intent().apply { putExtras(arg.extras.toBundle()) }
      WizardManagerHelper.copyWizardManagerExtras(srcIntentContainerForWizardManagerExtras, it)

      when (arg.action) {
        is Action.ResultAction -> it.putPersistableExtra(KEY_RESULT, arg.action.result)
        is Action.TaskAction -> {
          it.putPersistableExtra(KEY_TASK, arg.action.task)
          arg.action.onFailure?.let { onFailure ->
            it.putPersistableExtra(KEY_ON_FAILURE, onFailure)
          }
        }
      }
    }

  override fun performExtractArgument(intent: Intent): OrchestratorArgument =
    OrchestratorArgument(
      extras =
        intent.extras?.getPersistable(KEY_EXTRAS, WizardManagerExtras) ?: error("Missing extras"),
      action =
        if (intent.hasExtra(KEY_RESULT)) {
          Action.ResultAction(
            intent.extras?.getPersistable(KEY_RESULT, PersistableWizardResult)
              ?: error("Missing result")
          )
        } else if (intent.hasExtra(KEY_TASK)) {
          Action.TaskAction(
            intent.extras?.getPersistable(KEY_TASK, PendingContract.Task) ?: error("Missing task"),
            intent.extras?.getPersistable(KEY_ON_FAILURE, PendingContract.Activity),
          )
        } else {
          error("Missing action")
        },
    )

  private const val INTENT_ACTION = "com.android.wizard.ORCHESTRATOR"
  private const val KEY_EXTRAS = "extras"
  private const val KEY_RESULT = "result"
  private const val KEY_TASK = "task"
  private const val KEY_ON_FAILURE = "fallback"
}
