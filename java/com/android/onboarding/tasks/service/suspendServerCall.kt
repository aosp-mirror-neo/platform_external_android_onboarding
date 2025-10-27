package com.android.onboarding.tasks.service

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Represents callback-based server call. */
fun interface ServerCall<R> {
  /**
   * Call the server API and report the results via callbacks.
   *
   * @param onSuccess Callback for successful result [R].
   * @param onError Callback for [OnboardingTaskServiceServerException] serialised as
   *   [IPersistableBundleStream]. Callers are expected to invoke `onSuccess` with successful call
   *   result [R] or `onError` with [IPersistableBundleStream] representing server error.
   */
  operator fun invoke(onSuccess: (R) -> Unit, onError: (IPersistableBundleStream) -> Unit)
}

/**
 * Suspends current coroutine and makes server [call].
 *
 * Callers are expected to invoke `onSuccess` with successful call result [R] or `onError` with
 * [IPersistableBundleStream] representing server error.
 *
 * @throws OnboardingTaskServiceServerException if `onError` callback was invoked.
 */
suspend fun <R> suspendServerCall(call: ServerCall<R>): R {
  var error: IPersistableBundleStream? = null
  var result: R? = null
  suspendCancellableCoroutine { cont ->
    call(
      onSuccess = {
        result = it
        cont.resume(Unit)
      },
      onError = {
        error = it
        cont.resume(Unit)
      },
    )
  }
  return if (error != null) {
    throw OnboardingTaskServiceException.fromPersistableBundle(error)
  } else {
    // This will never be null, but the compiler isn't smart enough to figure it out.
    checkNotNull(result)
  }
}
