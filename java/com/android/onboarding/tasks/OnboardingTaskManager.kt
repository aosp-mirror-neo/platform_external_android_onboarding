package com.android.onboarding.tasks

import androidx.annotation.VisibleForTesting
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.pending.BackgroundLaunchableForResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Manages the execution and state of onboarding tasks within the onboarding process. This interface
 * provides a set of functions for triggering a task, monitoring, and obtaining results from
 * onboarding tasks.
 */
interface OnboardingTaskManager : ComponentRef, AutoCloseable {

  /**
   * A [NodeId] associated with this task manager.
   *
   * Note that tasks executed via this task manager will always have their own different [NodeId].
   */
  val id: NodeId

  /**
   * Scope used for running tasks.
   *
   * This should only be used by Onboarding infrastructure.
   */
  @InternalOnboardingApi val coroutineScope: CoroutineScope

  /**
   * Closes this resource, relinquishing any underlying resources.
   *
   * This [OnboardingTaskManager] instance cannot be reused once this method is called.
   *
   * @see java.lang.AutoCloseable.close
   */
  override fun close() = Unit

  /**
   * Clears the cached task states.
   *
   * This is a test-only function to simulate losing states due to the app being killed. In real
   * life the cached task states will be lost when the app is killed, e.g. due to low memory, or
   * device reboot.
   */
  @VisibleForTesting fun clearCachedTaskStates()

  /**
   * Schedules an onboarding task execution asynchronously and returns a token for subsequent status
   * queries.
   * > **Note:** Method only suspends until the task scheduling is complete and not until the task
   * > execution itself is completed.
   *
   * @param contract The contract defining the task's arguments and result for the onboarding task.
   * @param input A defined argument for the onboarding task.
   * @return The token associated with the initiated onboarding task.
   */
  suspend fun <I, O> runTask(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O>

  /**
   * The equivalent to [runTask] except that if the result cannot be determined entirely in a
   * synchronous manner then null will be returned and no state will be changed.
   */
  fun <I, O> runTaskSynchronous(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O>?

  /**
   * Behaves like [runTask], or returns an existing [OnboardingTaskToken] for any executing or
   * executed task [contract] and [input] combination.
   *
   * Returns the token for the latest execution when there are multiple of them with the same
   * contract and input combination.
   *
   * Note: Currently, the executing or executed tasks will be forgotten when the app dies. In this
   * case, this behaves like [runTask].
   */
  suspend fun <I, O> runOrResumeTask(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O>

  /**
   * The equivalent to [runOrResumeTask] except that if the result cannot be determined entirely in
   * a synchronous manner then null will be returned.
   */
  fun <I, O> runOrResumeTaskSynchronous(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O>?

  /** @see runTask */
  suspend fun <O> runTask(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): OnboardingTaskToken<O> = runTask(contract, Unit)

  /** @see runTaskSynchronous */
  fun <O> runTaskSynchronous(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): OnboardingTaskToken<O>? = runTaskSynchronous(contract, Unit)

  /** @see runOrResumeTask */
  suspend fun <O> runOrResumeTask(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): OnboardingTaskToken<O> = runOrResumeTask(contract, Unit)

  /** @see runOrResumeTaskSynchronous */
  fun <O> runOrResumeTaskSynchronous(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): OnboardingTaskToken<O>? = runOrResumeTaskSynchronous(contract, Unit)

  /**
   * Non-suspend version of [runTask] which schedules a task and returns a token.
   *
   * @see runTask
   */
  fun <I, O> runTaskAsync(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): ListenableFuture<OnboardingTaskToken<O>>

  /** Non-suspend version of [runOrResumeTask]. */
  fun <I, O> runOrResumeTaskAsync(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): ListenableFuture<OnboardingTaskToken<O>>

  /** @see runTaskAsync */
  fun <O> runTaskAsync(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): ListenableFuture<OnboardingTaskToken<O>> = runTaskAsync(contract, Unit)

  /** @see runOrResumeTaskAsync */
  fun <O> runOrResumeTaskAsync(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): ListenableFuture<OnboardingTaskToken<O>> = runOrResumeTaskAsync(contract, Unit)

  /**
   * Executes an onboarding task asynchronously and waits for its completion, providing the final
   * result.
   *
   * @param contract The contract defining the task's arguments and result for the onboarding task.
   * @param input A defined argument for the onboarding task.
   * @return The final result of the onboarding task.
   */
  suspend fun <I, O> runTaskAndGetResult(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskState.Completed<O>

  /**
   * The equivalent to [runTaskAndGetResult] except that if the result cannot be determined entirely
   * in a synchronous manner then null will be returned.
   */
  fun <I, O> runTaskAndGetResultSynchronous(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskState.Completed<O>? =
    runTaskSynchronous(contract, input)?.let { waitForCompletedSynchronous(it) }

  /**
   * Behaves like [runTaskAndGetResult], or waits for an existing task's completion with the same
   * [contract] and [input] combination.
   *
   * The latest execution will be considered when there are multiple of them with the same contract
   * and input combination.
   *
   * Note: Currently, the executing or executed tasks will be forgotten when the app dies. In this
   * case, this behaves like [runTaskAndGetResult].
   */
  suspend fun <I, O> runOrResumeTaskAndGetResult(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskState.Completed<O>

  /**
   * The equivalent to [runOrResumeTaskAndGetResult] except that if the result cannot be determined
   * entirely in a synchronous manner then null will be returned.
   */
  fun <I, O> runOrResumeTaskAndGetResultSynchronous(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskState.Completed<O>? =
    runOrResumeTaskSynchronous(contract, input)?.let { waitForCompletedSynchronous(it) }

  /** @see runTaskAndGetResult */
  suspend fun <O> runTaskAndGetResult(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): OnboardingTaskState.Completed<O> = runTaskAndGetResult(contract, Unit)

  /** @see runTaskAndGetResultSynchronous */
  fun <O> runTaskAndGetResultSynchronous(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): OnboardingTaskState.Completed<O>? = runTaskAndGetResultSynchronous(contract, Unit)

  /** @see runOrResumeTaskAndGetResult */
  suspend fun <O> runOrResumeTaskAndGetResult(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): OnboardingTaskState.Completed<O> = runOrResumeTaskAndGetResult(contract, Unit)

  /** @see runOrResumeTaskAndGetResultSynchronous */
  fun <O> runOrResumeTaskAndGetResultSynchronous(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): OnboardingTaskState.Completed<O>? = runOrResumeTaskAndGetResultSynchronous(contract, Unit)

  /**
   * Executes an onboarding task asynchronously and waits for its completion, providing the final
   * result.
   *
   * @param contract The contract defining the task's arguments and result for the onboarding task.
   * @param input A defined argument for the onboarding task.
   * @return A [ListenableFuture] representing the final state of the onboarding task. The future
   *   encapsulates the asynchronous execution and completion of the task, allowing for the
   *   retrieval of the task state or result.
   */
  fun <I, O> runTaskAndGetResultAsync(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): ListenableFuture<OnboardingTaskState.Completed<O>>

  /** Non-suspend version of [runOrResumeTaskAndGetResult]. */
  fun <I, O> runOrResumeTaskAndGetResultAsync(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): ListenableFuture<OnboardingTaskState.Completed<O>>

  /** @see runTaskAndGetResultAsync */
  fun <O> runTaskAndGetResultAsync(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): ListenableFuture<OnboardingTaskState.Completed<O>> = runTaskAndGetResultAsync(contract, Unit)

  /** @see runOrResumeTaskAndGetResultAsync */
  fun <O> runOrResumeTaskAndGetResultAsync(
    contract: BackgroundLaunchableForResult<Unit, O>
  ): ListenableFuture<OnboardingTaskState.Completed<O>> =
    runOrResumeTaskAndGetResultAsync(contract, Unit)

  /**
   * Retrieves the current state of a previously initiated onboarding task.
   *
   * @param token The token associated with the onboarding task.
   * @return The result of the onboarding task.
   */
  suspend fun <O> getTaskState(token: OnboardingTaskToken<O>): OnboardingTaskState<O>

  /**
   * The equivalent to [getTaskState] except that if the state cannot be determined entirely in a
   * synchronous manner then null will be returned.
   */
  fun <O> getTaskStateSynchronous(token: OnboardingTaskToken<O>): OnboardingTaskState<O>?

  /** Java flavour of [getTaskState]. */
  fun <O> getTaskStateAsync(token: OnboardingTaskToken<O>): ListenableFuture<OnboardingTaskState<O>>

  /**
   * Waits for the result of a previously initiated onboarding task.
   *
   * If the provided [token] is not valid, the caller receives a null object, indicating that this
   * token is not recorded in the manager.
   *
   * If the [token] represents a task in a timeout state or encounters an exception, the caller
   * receives a [OnboardingTaskState.Failure] state.
   *
   * If the onboarding task completes successfully, the caller receives a
   * [OnboardingTaskState.Success] state with [Result] data.
   *
   * @param token The token associated with the onboarding task.
   * @return The final result of the onboarding task.
   */
  @CanIgnoreReturnValue
  suspend fun <O> waitForCompleted(token: OnboardingTaskToken<O>): OnboardingTaskState.Completed<O>

  /**
   * The equivalent to [waitForCompleted] except that if the result cannot be determined entirely in
   * a synchronous manner then null will be returned. This means that this method doesn't actually
   * wait for anything.
   */
  fun <O> waitForCompletedSynchronous(
    token: OnboardingTaskToken<O>
  ): OnboardingTaskState.Completed<O>?

  /**
   * Asynchronously waits for the result of a previously initiated onboarding task.
   *
   * If the provided [token] is not valid, the caller receives a null object, indicating that this
   * token is not recorded in the manager.
   *
   * If the [token] represents a task in a timeout state or encounters an exception, the caller
   * receives a [OnboardingTaskState.Failure] state.
   *
   * If the onboarding task completes successfully, the caller receives a
   * [OnboardingTaskState.Success] state with [Result] data.
   *
   * @param context application context for task execution.
   * @param token The token associated with the onboarding task.
   * @return A [ListenableFuture] containing the final result of the onboarding task.
   */
  fun <O> waitForCompletedAsync(
    token: OnboardingTaskToken<O>
  ): ListenableFuture<OnboardingTaskState.Completed<O>>

  /**
   * Returns a [Flow] to observe [OnboardingMetadataStore] updates for a task represented by
   * [token]. The flow completes when the task completes.
   *
   * To observe multiple tasks, you can call this API with different tokens and merge the flows with
   * [merge][kotlinx.coroutines.flow.merge].
   *
   * You should consider converting the flow into a [LiveData][androidx.lifecycle.LiveData] with
   * [asLiveData][androidx.lifecycle.asLiveData] for better integration with Android UI lifecycles.
   *
   * Returns an empty flow if a task cannot be found.
   */

  // compared to the
  //  time of invocation, will be returned or not.
  fun <O> getTaskMetadataFlow(token: OnboardingTaskToken<O>): Flow<OnboardingMetadataStore>

  /**
   * Returns the [OnboardingMetadataStore] sent by a task represented by [token] synchronously.
   *
   * You should only call this for tasks that were run synchronously with [runTaskSynchronous] or
   * alike. Otherwise, this could return an empty [OnboardingMetadataStore] when the task is not
   * complete. Use [getTaskMetadataFlow] to listen for metadata updates from tasks that were run
   * asynchronously.
   *
   * Returns null if the task cannot be found, the task doesn't support to be run synchronously, or
   * if the metadata cannot be fetched synchronously. E.g. when a task is run cross-process.
   */
  fun <O> getTaskMetadataSynchronous(token: OnboardingTaskToken<O>): OnboardingMetadataStore?

  /**
   * Interrupt a task with out-of-band information.
   *
   * The task can choose to use this information in any way. This could result in the task being
   * completed (successfully or unsuccessfully) immediately, or the task can continue executing.
   *
   * Note that use of this function should be very limited. This is not intended for arbitrary
   * communication with tasks. Excessive use of this functionality can make tasks difficult to
   * reason about.
   *
   * It is not guaranteed that the interrupt will be delivered to the task (for example, the task
   * might be terminating already) and if a valid task cannot be found, this will be a no-op.
   */
  suspend fun interrupt(token: OnboardingTaskToken<*>, reason: InterruptReason)

  /** Generates a new [OnboardingTaskToken] for given [contract] execution with [input]. */
  @VisibleForTesting
  fun <I, O> generateToken(
    contract: BackgroundLaunchableForResult<I, O>,
    input: I,
  ): OnboardingTaskToken<O>

  companion object
}
