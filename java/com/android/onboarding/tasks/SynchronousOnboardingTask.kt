package com.android.onboarding.tasks

import android.content.Context
import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.metadata.SensitiveString
import java.lang.ref.WeakReference

/**
 * An interface for [OnboardingTask] which contains logic that is explicitly fast enough to run in a
 * blocking manner on the UI thread at least some of the time.
 *
 * Callers can use [runTaskSynchronous] which will return null if a result cannot be calculated
 * synchronously, and can use [runTask] to calculate a result whether synchronously or not.
 *
 * It is permitted to call [runTaskSynchronous] to see if a result can be generated synchronously
 * and then not continue to [runTask].
 */
sealed interface OnboardingTaskWithSynchronousExecution<I, O, C : OnboardingTaskContract<I, O>> {

  /** The task that's being run. */
  val task: OnboardingTask<I, O, C>

  /**
   * Executes the onboarding task asynchronously.
   *
   * @param context the application context for the execution.
   * @param input The input arguments for the task.
   * @return The result of the onboarding task.
   */
  suspend fun runTask(context: Context, input: I): O

  /**
   * Executes the onboarding task synchronously.
   *
   * This should run fast enough that it is acceptable to run it on the UI thread.
   *
   * Null will be returned if a result cannot be calculated synchronously and no state will be
   * changed.
   */
  fun runTaskSynchronous(context: Context, input: I): O?

  /**
   * Run the task, converting the input from the provided [PersistableBundle].
   *
   * See [runTaskSynchronous] for details.
   */
  @InternalOnboardingApi
  fun runTaskSynchronous(context: Context, input: PersistableBundle): OnboardingTaskState<O>? {
    try {
      task.contextRef = WeakReference(context)
      task.isRunning = true
      val result = runTaskSynchronous(context, task.contract.inputDecoder.encode(input))
      if (result == null) {
        return result
      }
      return task.successInternal(result)
    } catch (e: Exception) {
      return task.failureInternal(
        SensitiveString("Task failed: ${e.message} (${e.stackTraceToString()})")
      )
    } finally {
      task.isRunning = false
    }
  }
}

/**
 * An interface for [OnboardingTask] which contains logic that is explicitly fast enough to run in a
 * blocking manner on the UI thread.
 *
 * Callers can use [runTask] or [runTaskSynchronous] and these will execute the same logic.
 */
interface SynchronousOnboardingTask<I, O, C> : OnboardingTaskWithSynchronousExecution<I, O, C>
  where C : OnboardingTaskContract<I, O> {
  override suspend fun runTask(context: Context, input: I): O = runTaskSynchronous(context, input)

  /**
   * Implements the logic of the task in a synchronous way.
   *
   * This should run fast enough that it is acceptable to run it on the UI thread.
   */
  abstract override fun runTaskSynchronous(context: Context, input: I): O
}

/**
 * An interface for [OnboardingTask] which contains logic that in some cases is explicitly fast
 * enough to run in a blocking manner on the UI thread but in other cases needs to be run
 * asynchronously like other tasks.
 *
 * When these tasks are executed, the [runTaskSynchronous] method will be called first. If it
 * returns a non-null value then that is treated as the result of the task, otherwise [runTaskAsync]
 * will be called.
 */
interface SometimesSynchronousOnboardingTask<I, O, C> :
  OnboardingTaskWithSynchronousExecution<I, O, C> where C : OnboardingTaskContract<I, O> {
  override suspend fun runTask(context: Context, input: I): O =
    runTaskSynchronous(context, input) ?: runTaskAsync(context, input)

  /**
   * Implements the logic of the task in a synchronous way.
   *
   * This should run fast enough that it is acceptable to run it on the UI thread.
   *
   * If this returns null then we are indicating that this task cannot be completed synchronously.
   * There is no guarantee that [runTaskAsync] is later called, so in these cases we must ensure
   * that state has not been changed. If this returns a non-null value then state modifications are
   * permitted but should be documented in the task contract.
   */
  abstract override fun runTaskSynchronous(context: Context, input: I): O?

  /** Implements the logic of the task for situations where it cannot be executed synchronously. */
  suspend fun runTaskAsync(context: Context, input: I): O
}
