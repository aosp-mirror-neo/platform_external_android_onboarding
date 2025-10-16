package com.android.onboarding.tasks.service

import android.os.IBinder
import android.os.IInterface
import android.os.PersistableBundle
import android.os.RemoteException
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.tasks.ERROR_RUNNING_TASK
import com.android.onboarding.tasks.ERROR_TASK_CANNOT_BE_FOUND
import com.android.onboarding.tasks.ErasedOnboardingTaskState
import com.android.onboarding.tasks.IGetTaskStateListener
import com.android.onboarding.tasks.IOnboardingTaskManagerService
import com.android.onboarding.tasks.IOnboardingTaskManagerServiceListener
import com.android.onboarding.tasks.OnboardingTaskState
import com.android.onboarding.tasks.OnboardingTaskToken
import com.android.onboarding.tasks.UnresolvedOnboardingTaskState
import com.android.onboarding.tasks.UnresolvedOnboardingTaskToken
import com.android.onboarding.utils.datasplitter.getPendingDataIdentifier
import com.android.onboarding.utils.datasplitter.getPendingDataSizeInBytes
import com.android.onboarding.utils.datasplitter.getPendingDataTotalChunks
import com.android.onboarding.utils.datasplitter.isPendingBundle
import com.android.onboarding.utils.datasplitter.isTooLargeForTransaction
import com.android.onboarding.utils.datasplitter.persistableBundleOfChunks
import com.android.onboarding.utils.datasplitter.removePendingBundleExtras
import com.android.onboarding.utils.datasplitter.splitForTransaction
import com.android.onboarding.utils.datasplitter.toPendingBundle
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.sizeInBytes
import com.android.onboarding.versions.OnboardingChanges
import com.android.onboarding.versions.changes.GET_TASK_STATE_WITH_CALLBACK_ID
import com.android.onboarding.versions.changes.SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield

private const val TAG = "OnboardingTaskServiceClient"
private const val TAG_COMPAT = "OnboardingTaskServiceClientCompat"

/** A client to interact with [OnboardingTaskService] via IPC. */
sealed interface OnboardingTaskServiceClient : OnboardingTaskService, ComponentRef {
  /**
   * A temporary implementation of [OnboardingTaskServiceClient] which calls legacy
   * [IOnboardingTaskManagerService] and approximates expected [OnboardingTaskServiceClient]
   * behaviour.
   *
   * This client has only two responsibilities - approximate expected [OnboardingTaskService]
   * behaviour via legacy [IOnboardingTaskManagerService] APIs and handle input and output data
   * conversion.
   */
  interface Compat : OnboardingTaskServiceClient {
    companion object {
      /**
       * Create a [OnboardingTaskServiceClient.Compat] instance.
       *
       * @param onboardingChanges The [OnboardingChanges] handle for feature set checks.
       * @param binder The binder returned from
       *   [Context.bindService][android.content.Context.bindService].
       */
      operator fun invoke(
        target: ComponentRef,
        onboardingChanges: OnboardingChanges,
        binder: IBinder,
      ): Compat {
        Log.d(TAG_COMPAT, "Client initialised for ${target.identity()}.")
        return OnboardingTaskServiceClientCompat(
          target,
          onboardingChanges,
          IOnboardingTaskManagerService.Stub.asInterface(binder),
        )
      }
    }
  }

  companion object {
    /**
     * Create a [OnboardingTaskServiceClient] instance.
     *
     * @param onboardingChanges The [OnboardingChanges] handle for feature set checks.
     * @param binder The binder returned from
     *   [Context.bindService][android.content.Context.bindService].
     */
    operator fun invoke(
      target: ComponentRef,
      onboardingChanges: OnboardingChanges,
      binder: IBinder,
    ): OnboardingTaskServiceClient {
      val service = IOnboardingTaskManagerService.Stub.asInterface(binder)
      val version =
        try {
          service.version()
        } catch (e: RemoteException) {
          // This should never happen actually, but we leave it just in case. Turns out AIDL proxy
          // only throws exceptions returned by the server and since the legacy server is alive and
          // well, but doesn't have `version` method to invoke, empty Parcel gets returned. Proxy
          // then checks for error headers in the parcel, finds none, and invokes Parcel.getInt for
          // result. There is obviously no int written to parcel, so that invocation returns default
          // value of 0, which works nicely for us.
          Log.w(TAG, "Failed to check server version - assuming version 0.", e)
          0
        }
      Log.v(TAG, "Server version $version.")
      return when (version) {
        0 -> {
          Log.d(TAG_COMPAT, "Client initialised for ${target.identity()}.")
          OnboardingTaskServiceClientCompat(target, onboardingChanges, service)
        }

        else -> {
          Log.d(TAG, "Client initialised for ${target.identity()}.")
          OnboardingTaskServiceClientImpl(target, service)
        }
      }
    }
  }
}

private class OnboardingTaskServiceClientImpl(
  target: ComponentRef,
  private val service: IOnboardingTaskManagerService,
) : OnboardingTaskServiceClient, IInterface by service, ComponentRef by target.asComponentRef() {
  override suspend fun startTask(token: OnboardingTaskToken<*>, input: PersistableBundle) =
    suspendServerCall { onSuccess, onError ->
      service.startTaskAsync(
        /* token = */ PersistableBundleStream(token),
        /* input = */ PersistableBundleStream(input),
        /* callback = */ IUnitCallback(onError = onError, onSuccess = { onSuccess(Unit) }),
      )
    }

  override suspend fun <O> getTaskState(token: OnboardingTaskToken<O>): OnboardingTaskState<O> =
    suspendServerCall { onSuccess, onError ->
        service.getTaskStateAsync(
          /* token = */ PersistableBundleStream(token),
          /* callback = */ IResultCallback(onError = onError, onSuccess = onSuccess),
        )
      }
      .await()
      .let(ErasedOnboardingTaskState::fromPersistableBundle)
      .resolve(token)

  override fun <O> observeTaskState(token: OnboardingTaskToken<O>): Flow<OnboardingTaskState<O>> =
    callbackFlow {
        val handle =
          service.observeTaskState(
            /* token = */ PersistableBundleStream(token),
            /* observer = */ IObserver(
              onClose = this@callbackFlow::close,
              onError = {
                launch {
                  this@callbackFlow.close(OnboardingTaskServiceException.fromPersistableBundle(it))
                }
              },
              onChange = {
                trySendBlocking(it).onFailure {
                  Log.w(
                    TAG,
                    "Failed to send state update while observing ${token.identity()}. Missed state: $it",
                  )
                }
              },
            ),
          )
        awaitClose { handle.close() }
      }
      .map(IPersistableBundleStream::await)
      .map(ErasedOnboardingTaskState::fromPersistableBundle)
      .map { it.resolve(token) }

  override suspend fun getTaskMetadata(token: OnboardingTaskToken<*>): OnboardingMetadataStore =
    suspendServerCall { onSuccess, onError ->
        service.getTaskMetadataAsync(
          /* token = */ PersistableBundleStream(token),
          /* callback = */ IResultCallback(onError = onError, onSuccess = onSuccess),
        )
      }
      .await()
      .let(OnboardingMetadataStore::fromPersistableBundle)

  override fun observeTaskMetadata(token: OnboardingTaskToken<*>): Flow<OnboardingMetadataStore> =
    callbackFlow {
        val handle =
          service.observeTaskMetadata(
            /* token = */ PersistableBundleStream(token),
            /* observer = */ IObserver(
              onClose = this@callbackFlow::close,
              onError = {
                launch {
                  this@callbackFlow.close(OnboardingTaskServiceException.fromPersistableBundle(it))
                }
              },
              onChange = {
                trySendBlocking(it).onFailure {
                  Log.w(
                    TAG,
                    "Failed to send metadata update while observing ${token.identity()}. Missed metadata: $it",
                  )
                }
              },
            ),
          )
        awaitClose { handle.close() }
      }
      .map(IPersistableBundleStream::await)
      .map(OnboardingMetadataStore::fromPersistableBundle)

  override suspend fun interruptTask(token: OnboardingTaskToken<*>, reason: InterruptReason): Unit =
    suspendServerCall { onSuccess, onError ->
      service.interruptTaskAsync(
        /* token = */ PersistableBundleStream(token),
        /* reason = */ PersistableBundleStream(reason),
        /* callback = */ IUnitCallback(onError = onError, onSuccess = { onSuccess(Unit) }),
      )
    }

  @CanIgnoreReturnValue
  override suspend fun <O> awaitTaskCompletion(
    token: OnboardingTaskToken<O>
  ): OnboardingTaskState.Completed<O> =
    suspendServerCall { onSuccess, onError ->
        service.awaitTaskCompletionAsync(
          /* token = */ PersistableBundleStream(token),
          /* callback = */ IResultCallback(onError = onError, onSuccess = onSuccess),
        )
      }
      .await()
      .let(ErasedOnboardingTaskState::fromPersistableBundle)
      .resolve(token) as OnboardingTaskState.Completed<O>
}

/**
 * Compat implementation of the new [OnboardingTaskServiceClient] using legacy AIDL APIs.
 *
 * No new methods should be overridden nor added to this class to allow for any potential future
 * AIDL addition to be delegated to [delegate].
 *
 * @param delegate The new client implementation to use for delegating all new AIDL APIs added after
 *   this implementation is merged.
 */
@Suppress("DEPRECATION") // We're intentionally calling deprecated APIs for compatibility.
@OptIn(InternalOnboardingApi::class)
private class OnboardingTaskServiceClientCompat(
  target: ComponentRef,
  private val onboardingChanges: OnboardingChanges,
  private val legacy: IOnboardingTaskManagerService,
  delegate: OnboardingTaskServiceClient = OnboardingTaskServiceClientImpl(target, legacy),
) : OnboardingTaskServiceClient.Compat, OnboardingTaskServiceClient by delegate {
  override suspend fun startTask(token: OnboardingTaskToken<*>, input: PersistableBundle) {
    Log.v(TAG_COMPAT, "startTask(${token.identity()}) Starting...")
    val erasedToken = token.toErased()
    val success = legacy.startTask(erasedToken, legacy.prepareData(erasedToken, input))
    if (!success) {
      Log.v(TAG_COMPAT, "startTask(${token.identity()}) Failed to start.")
      // Best-effort guess to approximate the proper errors returned by the new flow.
      throw OnboardingTaskServiceServerException(
        token,
        IllegalStateException(ERROR_TASK_CANNOT_BE_FOUND),
      )
    }
    yield()
    // Poll until the task is actually started.
    for (i in 0..<50) { // 50 x 100ms = 5s
      Log.v(TAG_COMPAT, "startTask(${token.identity()})[attempt=$i] Polling for InProgress.")
      val state = getTaskState(token).resolve(::error)
      if (
        state is OnboardingTaskState.Failure &&
          // ERROR_TASK_CANNOT_BE_FOUND at this point indicates that the task has not been started
          // yet.
          state.errorMessage.value.contains(ERROR_TASK_CANNOT_BE_FOUND)
      ) {
        Log.w(
          TAG_COMPAT,
          "startTask(${token.identity()})[$i][attempt=$i] Task has not stared yet, polling again.",
        )
        delay(100.milliseconds)
      } else {
        Log.v(TAG_COMPAT, "startTask(${token.identity()})[attempt=$i] Task has started.")
        return
      }
    }
    Log.w(TAG_COMPAT, "startTask(${token.identity()}) Task did not start in 5s, stopping polling.")
    throw OnboardingTaskServiceServerException(token, IllegalStateException(ERROR_RUNNING_TASK))
  }

  override suspend fun <O> getTaskState(token: OnboardingTaskToken<O>): OnboardingTaskState<O> {
    Log.v(TAG_COMPAT, "getTaskState(${token.identity()}) Fetching task state")
    val rawState =
      if (
        onboardingChanges.componentSupportsChange(
          token.nodeComponent,
          GET_TASK_STATE_WITH_CALLBACK_ID,
        )
      ) {
        Log.i(TAG_COMPAT, "getTaskState(${token.identity()}) Fetching task state with callback")
        suspendCancellableCoroutine { cont ->
          try {
            legacy.getTaskStateWithCallback(
              /* token = */ token.toErased(),
              /* listener = */ object : IGetTaskStateListener.Stub() {
                override fun onTaskStateObtained(state: ErasedOnboardingTaskState) {
                  cont.resume(state)
                }
              },
            )
          } catch (e: Exception) {
            Log.w(TAG_COMPAT, "getTaskState(${token.identity()}) Fetching task state failed", e)
            cont.resumeWithException(e)
          }
        }
      } else {
        Log.i(TAG_COMPAT, "getTaskState(${token.identity()}) Fetching task state without callback")
        legacy.getTaskState(token.toErased())
      }
    val state = legacy.restoreState(token, rawState).resolve(token)
    Log.v(TAG_COMPAT, "getTaskState(${token.identity()}) Fetched task state $state")
    return state
  }

  override fun <O> observeTaskState(token: OnboardingTaskToken<O>): Flow<OnboardingTaskState<O>> =
    callbackFlow {
        Log.v(TAG_COMPAT, "observeTaskState(${token.identity()}) Observing task state")
        launch {
          while (isActive) {
            Log.v(TAG_COMPAT, "observeTaskState(${token.identity()}) Polling state")
            trySend(getTaskState(token)).onFailure {
              Log.w(
                TAG_COMPAT,
                "Failed to send state update while observing ${token.identity()}. Cause: $it",
              )
            }
            delay(100.milliseconds)
          }
        }
        val finalState = awaitTaskCompletion(token)
        Log.v(
          TAG_COMPAT,
          "observeTaskState(${token.identity()}) Observed final task state $finalState",
        )
        send(finalState)
        this@callbackFlow.close()
      }
      // We can't trim this server-side on the legacy server so we do it here for consistency.
      .distinctUntilChanged()

  override suspend fun getTaskMetadata(token: OnboardingTaskToken<*>): OnboardingMetadataStore {
    Log.v(TAG_COMPAT, "getTaskMetadata(${token.identity()}) Fetching task metadata")
    val data = legacy.getTaskMetadata(token.toErased())
    val metadata =
      legacy.restoreData(token, data).let(OnboardingMetadataStore::fromPersistableBundle)
    Log.v(TAG_COMPAT, "getTaskMetadata(${token.identity()}) Fetched task metadata $metadata")
    return metadata
  }

  override fun observeTaskMetadata(token: OnboardingTaskToken<*>): Flow<OnboardingMetadataStore> =
    callbackFlow {
        Log.v(TAG_COMPAT, "observeTaskMetadata(${token.identity()}) Observing task metadata")
        val erasedToken = token.toErased()
        legacy.listenForTaskMetadataChanges(
          /* token = */ erasedToken,
          /* listener = */ object : IOnboardingTaskManagerServiceListener.Stub() {
            override fun onTaskCompleted(
              state: ErasedOnboardingTaskState,
              metadata: PersistableBundle,
            ) {
              Log.v(TAG_COMPAT, "observeTaskMetadata(${token.identity()}) Observer closed")
              trySend(state.lastMetadata ?: OnboardingMetadataStore()).onFailure {
                Log.w(
                  TAG_COMPAT,
                  "observeTaskMetadata(${token.identity()}) Failed to send metadata update on task completed. Cause: $it",
                )
              }
              close()
            }

            override fun onTaskMetadataUpdated(metadata: PersistableBundle) {
              val fullMetadata =
                legacy
                  .restoreData(erasedToken, metadata)
                  .let(OnboardingMetadataStore::fromPersistableBundle)
              Log.v(
                TAG_COMPAT,
                "observeTaskMetadata(${token.identity()}) Observed metadata $fullMetadata",
              )
              trySend(fullMetadata).onFailure {
                Log.w(
                  TAG_COMPAT,
                  "observeTaskMetadata(${token.identity()}) Failed to send metadata update while observing. Cause: $it",
                )
              }
            }
          },
        )

        awaitClose {
          // We should close the listener here, but unfortunately legacy API does not provide any
          // handles for us to do so
        }
      }
      .distinctUntilChanged()

  override suspend fun interruptTask(token: OnboardingTaskToken<*>, reason: InterruptReason) {
    Log.v(TAG_COMPAT, "interruptTask(${token.identity()}) Interrupting task")
    val erasedToken = token.toErased()
    legacy.interrupt(erasedToken, reason.toPersistableBundle())
    val state = getTaskState(token).resolve(::error)
    if (
      state is OnboardingTaskState.Failure && state.errorMessage.value == ERROR_TASK_CANNOT_BE_FOUND
    ) {
      Log.v(TAG_COMPAT, "interruptTask(${token.identity()}) Failed to interrupt task")
      // Approximating new behaviour for consistency when task was interrupted before it was started
      throw OnboardingTaskServiceServerException(
        token,
        IllegalStateException(ERROR_TASK_CANNOT_BE_FOUND),
      )
    }
  }

  @CanIgnoreReturnValue
  override suspend fun <O> awaitTaskCompletion(
    token: OnboardingTaskToken<O>
  ): OnboardingTaskState.Completed<O> {
    val result = suspendCancellableCoroutine { cont ->
      Log.v(TAG_COMPAT, "awaitTaskCompletion(${token.identity()}) Awaiting task completion")
      try {
        legacy.waitForCompleted(
          /* token = */ token.toErased(),
          /* listener = */ object : IOnboardingTaskManagerServiceListener.Stub() {
            override fun onTaskCompleted(
              state: ErasedOnboardingTaskState,
              metadata: PersistableBundle,
            ) {
              cont.resume(state)
            }

            override fun onTaskMetadataUpdated(metadata: PersistableBundle) {}
          },
        )
      } catch (e: Exception) {
        Log.w(TAG_COMPAT, "awaitTaskCompletion(${token.identity()}) Await failed", e)
        cont.resumeWithException(e)
      }
    }
    val state =
      legacy.restoreState(token, result).resolve(token) as OnboardingTaskState.Completed<O>
    Log.v(TAG_COMPAT, "awaitTaskCompletion(${token.identity()}) Task completed with $state")
    return state
  }

  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the [PersistableBundle] if it's small enough for binder transaction. Otherwise sends it
   * in chunks and returns its pending bundle representation.
   */
  private fun IOnboardingTaskManagerService.prepareData(
    token: UnresolvedOnboardingTaskToken<*>,
    data: PersistableBundle,
  ): PersistableBundle {
    if (!onboardingChanges.currentProcessSupportsChange(SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID)) {
      return data
    }
    if (!data.isTooLargeForTransaction()) {
      return data
    }
    val pendingDataBundle = data.toPendingBundle()
    // Sends the full input data in chunks.
    sendLargeDataInChunks(token, data, pendingDataBundle.getPendingDataIdentifier())
    // Send a placeholder instead of the full input data.
    return pendingDataBundle
  }

  private fun IOnboardingTaskManagerService.sendLargeDataInChunks(
    token: UnresolvedOnboardingTaskToken<*>,
    input: PersistableBundle,
    id: Int,
  ) {
    val chunks = input.splitForTransaction()
    Log.d(TAG_COMPAT, "Sending large input data id=$id in ${chunks.size} chunks.")
    for ((i, chunk) in chunks.withIndex()) {

      sendDataChunk(
        /* token = */ token.toErased(),
        /* dataId = */ id,
        /* chunk = */ chunk,
        /* chunkIndex = */ i,
        /* totalChunks = */ chunks.size,
        /* totalSizeInBytes = */ input.sizeInBytes(),
      )
    }
  }

  /**
   * Restores the [state] with the full data if necessary.
   *
   * Returns [state] unchanged if it's not a "pending state", or if the "send large data in chunks"
   * feature is disabled.
   */
  private fun <O> IOnboardingTaskManagerService.restoreState(
    token: OnboardingTaskToken<*>,
    state: UnresolvedOnboardingTaskState<O>,
  ): UnresolvedOnboardingTaskState<O> {
    val erasedState = state.toErased()
    if (
      !onboardingChanges.currentProcessSupportsChange(SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID) ||
        !erasedState.isPendingState()
    ) {
      return state
    }
    val dataId = erasedState.getPendingDataIdentifier()
    Log.d(TAG_COMPAT, "Restoring pending state $dataId with full data.")
    val fullData =
      fetchFullData(
        token = token,
        id = dataId,
        totalChunks = erasedState.getPendingDataTotalChunks(),
        totalSizeInBytes = erasedState.getPendingDataSizeInBytes(),
      )
    val restoredState = erasedState.restoreFullDataForPendingState(fullData)
    Log.d(TAG_COMPAT, "Restored pending state $dataId with full data.")
    return restoredState
  }

  /**
   * Restores the [data] with the full data if necessary.
   *
   * Returns [data] unchanged if it's not a "pending data", or if the "send large data in chunks"
   * feature is disabled.
   */
  private fun IOnboardingTaskManagerService.restoreData(
    token: UnresolvedOnboardingTaskToken<*>,
    data: PersistableBundle,
  ): PersistableBundle {
    if (
      !onboardingChanges.currentProcessSupportsChange(SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID) ||
        !data.isPendingBundle()
    ) {
      return data
    }
    val dataId = data.getPendingDataIdentifier()
    Log.d(TAG_COMPAT, "Restoring pending data $dataId with full data.")
    val fullData =
      fetchFullData(
        token = token,
        id = dataId,
        totalChunks = data.getPendingDataTotalChunks(),
        totalSizeInBytes = data.getPendingDataSizeInBytes(),
      )
    val restoredData = fullData.removePendingBundleExtras()
    Log.d(TAG_COMPAT, "Restored pending data $dataId with full data.")
    return restoredData
  }

  private fun IOnboardingTaskManagerService.fetchFullData(
    token: UnresolvedOnboardingTaskToken<*>,
    id: Int,
    totalChunks: Int,
    totalSizeInBytes: Int,
  ): PersistableBundle {
    Log.d(TAG_COMPAT, "Fetching full data of id: $id")
    val chunks = mutableListOf<ByteArray>()
    for (i in 0..<totalChunks) {
      val chunk =
        fetchDataChunk(
          /* token = */ token.toErased(),
          /* dataId = */ id,
          /* chunkIndex = */ i,
          /* totalChunks = */ totalChunks,
          /* totalSizeInBytes = */ totalSizeInBytes,
        )
      chunks.add(chunk)
    }
    return persistableBundleOfChunks(chunks)
  }
}
