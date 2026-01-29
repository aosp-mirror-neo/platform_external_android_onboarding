package com.google.android.setupwizard.waiting

import android.content.Context
import android.content.Intent
import com.android.onboarding.contracts.VoidOnboardingActivityApiContract
import com.android.onboarding.pending.PendingContract
import com.google.android.wizardmanager.WizardAction
import com.google.android.wizardmanager.WizardBundle
import com.google.android.wizardmanager.WizardManagerExtras
import com.google.android.wizardmanager.WizardStack
import com.google.android.wizardmanager.wizardManagerExtras

/** Interface for providing a waiting screen. */
interface WaitingScreenProvider {
  /** Returns a waiting screen. This will be the waiting screen to use when making the user wait. */
  fun getWaitingScreen(): WaitingScreen<out WaitingScreenArgument>
}

/** The arguments required to launch a waiting screen. */
abstract class WaitingScreenArgument {
  /** The extras to use for the waiting screen. */
  abstract val extras: WizardManagerExtras
  /** The task intended to be executed with the waiting screen, if any. */
  abstract val task: PendingContract.Task<*>?
}

/**
 * A waiting screen than can be used in setup wizard.
 *
 * @param T The type of the waiting screen argument.
 */
interface WaitingScreen<T : WaitingScreenArgument> {
  /** The contract for launching the waiting screen activity. */
  val contract: WaitingScreenContract<T>

  /** Creates the argument for the waiting screen from the original intent and wizard context. */
  fun createArgument(originalIntent: Intent, stack: WizardStack, wizardAction: WizardAction): T

  /** Creates the argument from a generic waiting screen argument. */
  fun createArgument(arg: WaitingScreenArgument): T

  /** Creates the wizard manager extras for the waiting screen. */
  fun createExtras(
    originalIntent: Intent,
    stack: WizardStack,
    wizardAction: WizardAction,
  ): WizardManagerExtras {
    val originalExtras = originalIntent.wizardManagerExtras
    val newWizardBundle = WizardBundle(stack, wizardAction)
    return WizardManagerExtras(
      newWizardBundle,
      originalExtras.setupWizardExtras,
      originalExtras.pendingActivityMetadata,
    )
  }

  /** Launches the waiting screen activity. */
  fun launch(
    context: Context,
    originalIntent: Intent,
    stack: WizardStack,
    wizardAction: WizardAction,
  )

  /** Launches the waiting screen activity with the provided argument. */
  fun launch(context: Context, arg: WaitingScreenArgument)

  /** Creates the intent to launch the waiting screen. */
  fun createIntent(
    context: Context,
    originalIntent: Intent,
    stack: WizardStack,
    wizardAction: WizardAction,
  ): Intent
}

/** The contract for the waiting screen activity. */
abstract class WaitingScreenContract<T : WaitingScreenArgument> :
  VoidOnboardingActivityApiContract<T>()
