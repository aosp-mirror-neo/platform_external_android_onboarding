package com.android.onboarding.contracts.setupwizard.orchestrator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import com.android.onboarding.contracts.VoidOnboardingActivityApiContract
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistableExtra
import com.android.onboarding.wizardmanager.actions.PersistableWizardResult
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.android.wizardmanager.WizardManagerExtras

/**
 * Base class for SetupWizard orchestrator contracts. This contract launches an orchestrator
 * activity which controls screen flow during onboarding. This screen is responsible for controlling
 * the flow of screens throughout Setup Wizard. It fulfils the requirements of an Onboarding
 * Orchestrator and so is able to execute Orchestrated Tasks.
 *
 * Subclasses are expected to override [activityComponentName] to point to the orchestrator activity
 * implementation, and optionally [intentAction] if required by the activity.
 *
 * @see IowaWaitingScreenContract
 * @see SpinnerWaitingScreenContract
 */
abstract class SetupWizardOrchestratorContract :
  VoidOnboardingActivityApiContract<SetupWizardOrchestratorContract.OrchestratorArgument>() {

  /** The component name of the orchestrator activity implementation. */
  abstract val activityComponentName: ComponentName

  /** The intent action required by the orchestrator activity, or null if not required. */
  abstract val intentAction: String?

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
    Intent().also {
      if (intentAction != null) {
        it.action = intentAction
      }
      it.component = activityComponentName
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

  private companion object {
    const val KEY_EXTRAS = "extras"
    const val KEY_RESULT = "result"
    const val KEY_TASK = "task"
    const val KEY_ON_FAILURE = "fallback"
  }
}
