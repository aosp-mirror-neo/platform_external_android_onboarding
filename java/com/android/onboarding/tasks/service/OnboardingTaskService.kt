package com.android.onboarding.tasks.service

import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.tasks.OnboardingTaskState
import com.android.onboarding.tasks.OnboardingTaskToken
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlinx.coroutines.flow.Flow

/**
 * A service to interact with tasks between components.
 *
 * This is a common API definition implemented by various classes to provide task execution
 * capabilities for both, local and IPC scenarios.
 *
 * Most common flows are these though:
 * - [com.android.onboarding.tasks.OnboardingTaskManager] ->
 *   [com.android.onboarding.tasks.OnboardingTaskManagerServiceBinder] ->
 *   [OnboardingTaskServiceServer]
 * - [com.android.onboarding.tasks.OnboardingTaskManager] ->
 *   [com.android.onboarding.tasks.CrossProcessTaskManager] -> [OnboardingTaskServiceClient] ->
 *   `IPC` -> [com.android.onboarding.tasks.OnboardingTaskManagerServiceBinder] ->
 *   [OnboardingTaskServiceServer]
 *
 * For more details see the design doc at
 * [___](http://___).
 *
 * @see OnboardingTaskServiceServer
 * @see OnboardingTaskServiceClient
 * @see com.android.onboarding.tasks.CrossProcessTaskManager
 */
interface OnboardingTaskService {
  /**
   * Start task identified by the [token] with provided [input] and suspend until it is ready to
   * start execution.
   */
  suspend fun startTask(token: OnboardingTaskToken<*>, input: PersistableBundle)

  /** Get current task state associated with the [token]. */
  suspend fun <O> getTaskState(token: OnboardingTaskToken<O>): OnboardingTaskState<O>

  /**
   * Observe task state associated with the [token] until it completes.
   *
   * Current task state as well as subsequent state updates will be published until completed state
   * is published and the flow is closed.
   */
  fun <O> observeTaskState(token: OnboardingTaskToken<O>): Flow<OnboardingTaskState<O>>

  /** Get current task metadata associated with the [token]. */
  suspend fun getTaskMetadata(token: OnboardingTaskToken<*>): OnboardingMetadataStore

  /**
   * Observe task metadata associated with the [token] until it completes.
   *
   * Current task metadata as well as subsequent metadata updates will be published until the task
   * completes and the flow is closed.
   */
  fun observeTaskMetadata(token: OnboardingTaskToken<*>): Flow<OnboardingMetadataStore>

  /**
   * Passes an interruption message to an executing task.
   *
   * If the token is not valid then this is no-op.
   *
   * @throws OnboardingTaskServiceServerException if the task has not been started.
   */
  suspend fun interruptTask(token: OnboardingTaskToken<*>, reason: InterruptReason)

  /**
   * Waits and returns the result of a previously initiated onboarding task.
   *
   * @param token pre-constructed local token assigned to the task.
   */
  @CanIgnoreReturnValue
  suspend fun <O> awaitTaskCompletion(
    token: OnboardingTaskToken<O>
  ): OnboardingTaskState.Completed<O>
}
