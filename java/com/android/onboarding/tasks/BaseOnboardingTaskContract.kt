package com.android.onboarding.tasks

import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.encoding.InputHandler
import com.android.onboarding.pending.BackgroundLaunchableForResult
import com.google.errorprone.annotations.CanIgnoreReturnValue

/**
 * Common base for all [OnboardingTaskContract]. Public clients always look to use
 * [OnboardingTaskContract] as all other implementations are meant for internal use only.
 *
 * @see OnboardingTaskContract
 */

abstract class BaseOnboardingTaskContract<I, O> @InternalOnboardingApi constructor() :
  BackgroundLaunchableForResult<I, O>, InputHandler<PersistableBundle, I> {

  /**
   * Validates the integrity of the provided task arguments.
   *
   * This function checks whether the provided task arguments are valid and returns a Boolean
   * indicating the validation result.
   *
   * @param taskArgs The task arguments to validate.
   * @return `true` if the task arguments are valid, `false` otherwise.
   */
  @CanIgnoreReturnValue open fun validate(taskArgs: I?): Boolean = true

  /**
   * Returns whether this contract will accept an interrupt for the given [reason].
   *
   * How an interrupt is handled is defined by the task implementation, in the [onInterrupt] method.
   *
   * The accepted interrupt reasons are defined in the
   * [com.android.onboarding.contracts.annotations.OnboardingNode] annotation. Specifically, the
   * [com.android.onboarding.contracts.annotations.OnboardingNode.background.acceptsInterrupts]
   * property.
   */
  abstract fun canAcceptInterrupt(reason: InterruptReason): Boolean
}
