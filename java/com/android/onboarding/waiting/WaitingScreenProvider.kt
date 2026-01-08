package com.google.android.setupwizard.waiting

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
  fun getWaitingScreen(): WaitingScreen<out WaitingScreenArgument>
}

/** The arguments required to launch a waiting screen. */
abstract class WaitingScreenArgument {
  abstract val extras: WizardManagerExtras
  abstract val task: PendingContract.ErasedTask?
}

/**
 * A waiting screen than can be used in setup wizard.
 *
 * @param T The type of the waiting screen argument.
 */
interface WaitingScreen<T : WaitingScreenArgument> {
  val contract: WaitingScreenContract<T>

  fun createArgument(originalIntent: Intent, stack: WizardStack, wizardAction: WizardAction): T

  fun createArgument(arg: WaitingScreenArgument): T

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
}

abstract class WaitingScreenContract<T : WaitingScreenArgument> :
  VoidOnboardingActivityApiContract<T>()
