package com.android.onboarding.utils

import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Process
import com.android.onboarding.utils.logging.OnboardingLog as Log

/**
 * Safely bind to [serviceIntent].
 *
 * This gracefully handles any error thrown by [Context.bindService].
 *
 * Returns if we have successfully bound to [serviceIntent].
 */
fun ServiceConnection.safeBindService(context: Context, serviceIntent: Intent): Boolean {
  val bound =
    try {
      context.bindService(serviceIntent, this, BIND_AUTO_CREATE)
    } catch (e: Exception) {
      Log.w(
        TAG,
        "${context.packageName} with UID ${Process.myUid()} failed to bind service $serviceIntent",
        e,
      )
      false
    }
  if (!bound) {
    try {
      context.unbindService(this)
    } catch (e: Exception) {
      // This is a best-effort cleanup to align to best practices. We shouldn't risk crashing
      // doing this.
      Log.w(
        TAG,
        "${context.packageName} with UID ${Process.myUid()} failed to unbind service as a part of clean up: $serviceIntent",
        e,
      )
    }
  }
  return bound
}

/**
 * Safely unbinds the service connection.
 *
 * This gracefully handles any error thrown by [Context.unbindService].
 */
fun ServiceConnection.safeUnbindService(context: Context) {
  try {
    context.unbindService(this)
  } catch (e: Exception) {
    Log.w(TAG, "Ignoring failure to unbind service.", e)
  }
}

private const val TAG = "ServiceConnection"
