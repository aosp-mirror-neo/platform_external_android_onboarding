package com.android.onboarding.tasks

import com.android.onboarding.datastore.OnboardingDataStore
import com.android.onboarding.metadata.NonSensitiveString
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.nodes.UniqueNodeRef
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.PersistableMapDeserializer
import com.android.onboarding.utils.persistable.PersistableMapSerializer
import com.android.onboarding.versions.OnboardingChanges
import com.android.onboarding.versions.changes.STORE_TASK_STATES_CHANGE_ID
import java.lang.System.identityHashCode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transformWhile

/**
 * Manages the states of onboarding tasks in a centralized and thread-safe manner.
 *
 * [OnboardingTaskStateManager] allows you to track/update the progress and results of individual
 * onboarding tasks, identified by unique [UniqueNodeRef] instances.
 *
 * The states are persisted on the host side. In an exceptional case, a failure state will be
 * persisted on the client side when the service between the client and the host can't be
 * established.
 */
class OnboardingTaskStateManager(
  private val onboardingChanges: OnboardingChanges,
  private val taskDataStore: OnboardingDataStore,
  private val coroutineScope: CoroutineScope,
) : AutoCloseable {
  private val taskStates =
    ConcurrentHashMap<UniqueNodeRef.Stable, MutableStateFlow<OnboardingTaskState<*>>>()
  /** In-progress metadata. This is not persisted to disk. */
  private val taskMetadata = ConcurrentHashMap<UniqueNodeRef.Stable, OnboardingMetadataStore>()
  private val taskMetadataChannels =
    ConcurrentHashMap<UniqueNodeRef.Stable, Channel<OnboardingMetadataStore>>()

  /** A record of all task instances created for running tasks. */
  private val taskInstances =
    ConcurrentHashMap(mapOf<UniqueNodeRef.Stable, OnboardingTask<*, *, *>>())

  /** A record of all task execution instances. */
  private val taskExecutions = ConcurrentHashMap(mapOf<UniqueNodeRef.Stable, Job>())

  private var restoreDeferredLazy = scheduleRestoreStates()

  /** Ongoing disk writes to flush. */
  private val diskWrites = ConcurrentLinkedQueue<Deferred<*>>()

  /**
   * Record an instance of the [task] for a given [token].
   *
   * This will replace any previously cached instances for the same [token].
   */
  fun cacheTaskInstance(token: UniqueNodeRef, task: OnboardingTask<*, *, *>) {

    taskInstances[token.asUniqueNodeRef()] = task
  }

  /** Returns a previous instantiated task instance for [token], or null if there isn't one. */
  fun getTaskInstance(token: UniqueNodeRef) = taskInstances[token.asUniqueNodeRef()]

  /**
   * Record a [Deferred] instance of the task execution for a given [token].
   *
   * This will replace any previously cached instances for the same [token].
   *
   * @see OnboardingTaskManagerServiceBinder.executeTask
   */
  fun cacheTaskExecution(token: UniqueNodeRef, taskExecution: Job) {

    taskExecutions[token.asUniqueNodeRef()] = taskExecution
  }

  /**
   * Returns a previous instantiated task execution [Deferred] instance for [token], or null if
   * there isn't one.
   */
  fun getTaskExecution(token: UniqueNodeRef): Job? = taskExecutions[token.asUniqueNodeRef()]

  /**
   * Updates the state of an onboarding task.
   *
   * @param token The [UnresolvedOnboardingTaskToken] identifying the task.
   * @param state The updated [OnboardingTaskState] object representing the task's new state. The
   *   type of the result within the state object is flexible.
   */
  operator fun <O> set(token: UnresolvedOnboardingTaskToken<O>, state: OnboardingTaskState<O>) {
    Log.v(TAG, "[${identityHashCode(this)}] Writing state $state for $token")
    taskStates.getOrPut(token.asUniqueNodeRef()) { MutableStateFlow(state) }.value = state
    if (onboardingChanges.currentProcessSupportsChange(STORE_TASK_STATES_CHANGE_ID)) {
      diskWrites.add(coroutineScope.async { persistTaskStates() })
    }
  }

  /**
   * Updates the metadata of an onboarding task.
   *
   * @param token The [UniqueNodeRef] identifying the task.
   * @param metadata The updated metadata.
   */
  fun setMetadata(token: UniqueNodeRef, metadata: OnboardingMetadataStore) {
    taskMetadata[token.asUniqueNodeRef()] = metadata
    // Note: We don't need to persist metadata here, because we only care about the last submitted
    // metadata, which will be persisted in [set] when the task completes.
  }

  /**
   * Initiates a metadata channel associated with a task identified by [token].
   *
   * @see emitMetadata
   * @see getMetadataChannel
   */
  fun initMetadataChannel(token: UniqueNodeRef) {
    taskMetadataChannels[token.asUniqueNodeRef()] =
      Channel<OnboardingMetadataStore>(
          capacity = Channel.UNLIMITED,
          onUndeliveredElement = { metadata -> Log.w(TAG, "Undelivered: $metadata [$token]") },
        )
        .also {
          it.invokeOnClose { cause ->
            Log.d(TAG, "Metadata channel closed with cause: $cause [$token]")
          }
        }
  }

  /**
   * Closes and clears the metadata channel associated with [token].
   *
   * @see [initMetadataChannel]
   */
  fun closeMetadataChannel(token: UniqueNodeRef) {
    taskMetadataChannels.remove(token.asUniqueNodeRef())?.close()
  }

  /**
   * Emits [metadata] by a task identified by [token] into a [Channel] that was initiated by
   * [initMetadataChannel].
   *
   * @see [getMetadataChannel]
   */
  fun emitMetadata(token: UniqueNodeRef, metadata: OnboardingMetadataStore) {
    // Pass by value, so that we get distinct objects when receiving after consecutive sends.
    val result = taskMetadataChannels[token.asUniqueNodeRef()]?.trySend(metadata.copy())
    if (result?.isSuccess != true) {
      Log.w(TAG, "Failed to emit metadata. result=$result [$token]")
    }
  }

  /**
   * Gets a [Channel] of metadata, emitted by the task identified by [token].
   *
   * The channel closes when the task completes.
   *
   * @see [emitMetadata]
   */
  @Deprecated(
    message = "Use observeMetadata which supports multiple observers.",
    replaceWith = ReplaceWith(expression = "observeMetadata"),
  )
  fun getMetadataChannel(token: UniqueNodeRef): Channel<OnboardingMetadataStore>? =
    taskMetadataChannels[token.asUniqueNodeRef()]

  /**
   * Gets a [Flow] of metadata, emitted by the task identified by [token].
   *
   * @see [emitMetadata]
   */
  fun observeMetadata(token: UniqueNodeRef): Flow<OnboardingMetadataStore>? =
    taskMetadataChannels[token.asUniqueNodeRef()]?.receiveAsFlow()

  /**
   * Gets a [Flow] of task state identified by [token], which emits updates each time it changes.
   *
   * State changes will be emitted until task reaches [OnboardingTaskState.Completed] state, which
   * will be emitted as the last element before the flow closes.
   */
  fun observeState(token: UniqueNodeRef): Flow<OnboardingTaskState<*>>? =
    taskStates[token.asUniqueNodeRef()]?.asStateFlow()?.transformWhile {
      emit(it)
      it !is OnboardingTaskState.Completed<*>
    }

  /**
   * Retrieves the current state of an onboarding task.
   *
   * @param token The [UnresolvedOnboardingTaskToken] identifying the task.
   * @return The [UnresolvedOnboardingTaskState] object associated with the task. If no state is
   *   found for the provided token, a [OnboardingTaskState.Failure] with the error message
   *   [ERROR_TASK_CANNOT_BE_FOUND] is returned.
   */
  operator fun <O> get(token: UnresolvedOnboardingTaskToken<O>): OnboardingTaskState<O> =
    getOrNull(token)
      ?: run {
        Log.w(TAG, "[${identityHashCode(this)}] No state present for $token")
        OnboardingTaskState.Failure(token, NonSensitiveString(ERROR_TASK_CANNOT_BE_FOUND))
      }

  /**
   * Retrieves the metadata of an onboarding task.
   *
   * @param token The [UniqueNodeRef] identifying the task.
   * @return The task metadata.
   */
  fun getMetadata(token: UniqueNodeRef): OnboardingMetadataStore =
    taskMetadata[token.asUniqueNodeRef()] ?: OnboardingMetadataStore()

  /**
   * Retrieves the current state of an onboarding task.
   *
   * @param token The [UnresolvedOnboardingTaskToken] identifying the task.
   * @return The [UnresolvedOnboardingTaskState] object associated with the task or null if no state
   *   is found for the provided token.
   */
  fun <O> getOrNull(token: UnresolvedOnboardingTaskToken<O>): OnboardingTaskState<O>? =
    taskStates[token.asUniqueNodeRef()]?.value?.unErase()

  @Suppress("UNCHECKED_CAST")
  private fun <O> OnboardingTaskState<*>.unErase() = this as OnboardingTaskState<O>


  override fun close() {
    taskStates.clear()
    for ((_, v) in taskMetadataChannels) v.close()
    taskMetadataChannels.clear()
    taskInstances.clear()
    taskExecutions.clear()
    restoreDeferredLazy = scheduleRestoreStates()
    diskWrites.clear()
  }

  private suspend fun persistTaskStates() {
    if (!onboardingChanges.currentProcessSupportsChange(STORE_TASK_STATES_CHANGE_ID)) {
      return
    }
    restoreTaskStatesIfNecessary()
    Log.d(TAG, "Persisting task states")
    val states = taskStates.mapValues { (_, state) -> state.value }.toMap()
    taskDataStore.write(KEY_TASK_STATES, states, PersistableMapSerializer)
    Log.d(TAG, "Persisted task states: $states")
  }

  private fun scheduleRestoreStates(): Lazy<Deferred<Unit>> {
    return lazy {
      coroutineScope.async {
        if (!onboardingChanges.currentProcessSupportsChange(STORE_TASK_STATES_CHANGE_ID)) {
          return@async
        }
        taskDataStore
          .read(
            KEY_TASK_STATES,
            PersistableMapDeserializer(UniqueNodeRef, ErasedOnboardingTaskState),
          )
          ?.let {
            Log.d(TAG, "Restored task states from disk: $it")
            taskStates.putAll(
              it.mapValues { (key, state) -> MutableStateFlow(state.resolve { key }) }
            )
          }
        return@async
      }
    }
  }

  suspend fun restoreTaskStatesIfNecessary() {
    if (!onboardingChanges.currentProcessSupportsChange(STORE_TASK_STATES_CHANGE_ID)) {
      return
    }
    restoreDeferredLazy.value.await()
  }

  /**
   * Flush ongoing disk writes.
   *
   * This needs to be done before an API that writes to the disk returns. Because the next binder
   * instance may need to access the info from disk immediately after.
   */
  suspend fun flush() {
    if (!onboardingChanges.currentProcessSupportsChange(STORE_TASK_STATES_CHANGE_ID)) {
      return
    }
    Log.v(TAG, "Attempting to flush: $diskWrites")
    while (diskWrites.isNotEmpty()) {
      diskWrites.poll()?.let {
        if (it.isActive) {
          Log.d(TAG, "Flushing disk write: $it")
          it.await()
        } else {
          Log.v(TAG, "Not flushing inactive disk write: $it")
        }
      }
    }
  }

  private companion object {
    const val TAG = "TaskStateManager"

    const val KEY_TASK_STATES = "task_states"
  }
}
