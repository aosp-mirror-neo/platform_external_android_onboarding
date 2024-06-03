package com.android.onboarding.tasks

import android.os.PersistableBundle
import com.google.errorprone.annotations.CanIgnoreReturnValue

/**
 * Abstract class representing the contract for an onboarding task.
 *
 * This class defines the contract for an onboarding task, specifying how task arguments and results
 * should be handled. Implementing classes are expected to provide concrete implementations for
 * encoding and extracting task arguments/results.
 *
 * @param TaskArgsT The type representing the task arguments.
 * @param TaskResultT The type representing the task result.
 */
abstract class OnboardingTaskContract<TaskArgsT, TaskResultT> {
  /**
   * Returns the package name associated with this contract.
   *
   * @return The package name.
   */
  abstract val packageName: String

  /**
   * Validates the integrity of the provided task arguments.
   *
   * This function checks whether the provided task arguments are valid and returns a Boolean
   * indicating the validation result.
   *
   * @param taskArgs The task arguments to validate.
   * @return `true` if the task arguments are valid, `false` otherwise.
   */
  @CanIgnoreReturnValue abstract fun validate(taskArgs: TaskArgsT?): Boolean

  /**
   * Encodes task arguments into a [PersistableBundle] object.
   *
   * @param taskArgs The task arguments to encode.
   * @return The encoded [PersistableBundle].
   */
  abstract fun encodeArgs(taskArgs: TaskArgsT): PersistableBundle

  /**
   * Extracts task arguments from a [PersistableBundle] and returns them as an [TaskArgs].
   *
   * @param bundle The [PersistableBundle] containing the encoded task arguments.
   * @return The extracted task arguments.
   */
  abstract fun extractArgs(bundle: PersistableBundle): TaskArgsT

  /**
   * Encodes a task result into a [PersistableBundle].
   *
   * @param taskResult The task result to encode.
   * @return The encoded [PersistableBundle].
   */
  abstract fun encodeResult(taskResult: TaskResultT): PersistableBundle

  /**
   * Extracts task results from a [PersistableBundle] and returns them as a [TaskResult].
   *
   * @param bundle The [PersistableBundle] containing the encoded task results.
   * @return The extracted task results.
   */
  abstract fun extractResult(bundle: PersistableBundle): TaskResultT
}

/** Equivalent to [OnboardingTaskContract] for contracts which do not take arguments. */
abstract class ArgumentFreeOnboardingTaskContract<TaskResultT> :
  OnboardingTaskContract<Unit, TaskResultT>() {

  override fun validate(taskArgs: Unit?): Boolean = true

  override fun encodeArgs(taskArgs: Unit): PersistableBundle = PersistableBundle()

  override fun extractArgs(bundle: PersistableBundle) {
    // Do nothing as no arguments.
  }
}

/** Equivalent to [OnboardingTaskContract] for contracts which do not return result. */
abstract class VoidOnboardingTaskContract<TaskArgsT> : OnboardingTaskContract<TaskArgsT, Unit>() {

  override fun encodeResult(taskResult: Unit): PersistableBundle = PersistableBundle()

  override fun extractResult(bundle: PersistableBundle) {
    // Do nothing as no result.
  }
}

/**
 * Equivalent to [OnboardingTaskContract] for contracts which do not take arguments or return
 * results.
 */
abstract class ArgumentFreeVoidOnboardingTaskContract : ArgumentFreeOnboardingTaskContract<Unit>() {

  override fun encodeResult(taskResult: Unit): PersistableBundle = PersistableBundle()

  override fun extractResult(bundle: PersistableBundle) {
    // Do nothing as no result.
  }
}
