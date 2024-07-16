package com.android.onboarding.tasks

import android.content.Intent
import android.os.PersistableBundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.contracts.annotations.OnboardingNodeMetadata
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
   * Metadata associated with the onboarding task node, extracted from the component name.
   *
   * This includes the package name and an optional sub-name for further identification.
   */
  val onboardingTaskNodeMetaData: OnboardingNodeMetadata by lazy {
    OnboardingNode.getOnboardingNodeMetadata(this::class.java)
  }

  /** The `ComponentName` for this contract. */
  val componentName: String by lazy {
    OnboardingNode.extractComponentNameFromClass(this::class.java)
  }

  /** Returns the intent which connects to the corresponding service to run the [OnboardingTask]. */
  val taskServiceIntent: Intent by lazy {
    // If no sub module in the same package(such as GMS), then just use default intent action.
    val intentAction =
      if (onboardingTaskNodeMetaData.packageName == onboardingTaskNodeMetaData.component) {
        ACTION_PREFIX + ACTION_NAME
      } else {
        ACTION_PREFIX + onboardingTaskNodeMetaData.component + "." + ACTION_NAME
      }
    Intent(intentAction).setPackage(onboardingTaskNodeMetaData.packageName)
  }

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
   * Encodes task arguments additional contract information into a [PersistableBundle] object.
   *
   * @param args The task arguments to encode.
   * @return The encoded [PersistableBundle].
   */
  fun encodeArgs(args: TaskArgsT): PersistableBundle {
    val persistableBundle = performEncodeArgs(args)
    // We save the name of the contract class to facilitate creating an instance of the contract in
    // another process.
    persistableBundle.putString(EXTRA_CONTRACT_CLASS, this::class.java.name)
    return persistableBundle
  }

  /**
   * Encodes task arguments into a [PersistableBundle] object.
   *
   * @param taskArgs The task arguments to encode.
   * @return The encoded [PersistableBundle].
   */
  abstract fun performEncodeArgs(taskArgs: TaskArgsT): PersistableBundle

  /**
   * Extracts task arguments from a [PersistableBundle] and returns them as an [TaskArgsT].
   *
   * @param bundle The [PersistableBundle] containing the encoded task arguments.
   * @return The extracted task arguments.
   */
  fun extractArgs(bundle: PersistableBundle): TaskArgsT {
    val args = performExtractArgs(bundle)
    return args
  }

  /**
   * Extracts task arguments from a [PersistableBundle] and returns them as an [TaskArgsT].
   *
   * @param bundle The [PersistableBundle] containing the encoded task arguments.
   * @return The extracted task arguments.
   */
  abstract fun performExtractArgs(bundle: PersistableBundle): TaskArgsT

  /**
   * Encodes a task result into a [PersistableBundle].
   *
   * @param result The task result to encode.
   * @return The encoded [PersistableBundle].
   */
  fun encodeResult(taskResult: TaskResultT): PersistableBundle {
    return performEncodeResult(taskResult)
  }

  /**
   * Encodes a task result into a [PersistableBundle].
   *
   * @param taskResult The task result to encode.
   * @return The encoded [PersistableBundle].
   */
  abstract fun performEncodeResult(taskResult: TaskResultT): PersistableBundle

  /**
   * Extracts task results from a [PersistableBundle] and returns them as a [TaskResult].
   *
   * @param bundle The [PersistableBundle] containing the encoded task results.
   * @return The extracted task results.
   */
  fun extractResult(bundle: PersistableBundle): TaskResultT {
    return performExtractResult(bundle)
  }

  /**
   * Extracts task results from a [PersistableBundle] and returns them as a [Result].
   *
   * @param bundle The [PersistableBundle] containing the encoded task results.
   * @return The extracted task results.
   */
  abstract fun performExtractResult(bundle: PersistableBundle): TaskResultT

  companion object {
    const val EXTRA_CONTRACT_CLASS = "contract_class"
    private const val TAG = "OnboardingTaskContract"

    @VisibleForTesting const val ACTION_PREFIX = "com.android.onboarding.tasks."
    @VisibleForTesting const val ACTION_NAME = "RUN_ONBOARDING_TASK"

    /**
     * Attempts to create an instance of the specified contract class using reflection. This
     * function takes a Class object representing the contract class and attempts to instantiate it
     * using its default constructor. If successful, it returns the created instance; otherwise, it
     * logs an error and returns null.
     *
     * @param contractClass The Class object representing the contract class.
     * @return An instance of the contract class, or `null` if instantiation fails.
     */
    fun <T : OnboardingTaskContract<*, *>> tryCreateContractInstance(contractClass: Class<T>): T? {
      try {
        val constructor = contractClass.getDeclaredConstructor()
        return constructor.newInstance() as T
      } catch (e: Exception) {
        Log.w(TAG, "Error instantiating contract: $e")
      }
      return null
    }
  }
}

/** Equivalent to [OnboardingTaskContract] for contracts which do not take arguments. */
abstract class ArgumentFreeOnboardingTaskContract<TaskResultT> :
  OnboardingTaskContract<Unit, TaskResultT>() {

  override fun validate(taskArgs: Unit?): Boolean = true

  override fun performEncodeArgs(taskArgs: Unit): PersistableBundle = PersistableBundle()

  override fun performExtractArgs(bundle: PersistableBundle) {
    // Do nothing as no arguments.
  }
}

/** Equivalent to [OnboardingTaskContract] for contracts which do not return result. */
abstract class VoidOnboardingTaskContract<TaskArgsT> : OnboardingTaskContract<TaskArgsT, Unit>() {

  override fun performEncodeResult(taskResult: Unit): PersistableBundle = PersistableBundle()

  override fun performExtractResult(bundle: PersistableBundle) {
    // Do nothing as no result.
  }
}

/**
 * Equivalent to [OnboardingTaskContract] for contracts which do not take arguments or return
 * results.
 */
abstract class ArgumentFreeVoidOnboardingTaskContract : ArgumentFreeOnboardingTaskContract<Unit>() {

  override fun performEncodeResult(taskResult: Unit): PersistableBundle = PersistableBundle()

  override fun performExtractResult(bundle: PersistableBundle) {
    // Do nothing as no result.
  }
}
