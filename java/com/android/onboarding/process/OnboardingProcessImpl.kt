package com.android.onboarding.process

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import javax.inject.Singleton

/** Default production implementation of [OnboardingProcess]. */
@Singleton
@TargetApi(Build.VERSION_CODES.O)
class OnboardingProcessImpl(private val context: Context) : OnboardingProcess {

  override fun keepAlive(reason: KeepAliveReason): KeepAliveToken {
    context.startForegroundService(Intent(context, NotificationKeepAliveService::class.java))

    val keepAliveToken = KeepAliveToken()
    currentTokens.add(keepAliveToken)

    return keepAliveToken
  }

  /** This token is used by the calling process to terminate the KeepAlive Service API. */
  inner class KeepAliveToken : AutoCloseable {

    override fun close() {
      Log.d("OnboardingProcessImpl", "KeepAliveToken#close invoked - $this")

      currentTokens.remove(this)

      if (currentTokens.isEmpty()) {
        context.stopService(Intent(context, NotificationKeepAliveService::class.java))
      }
    }
  }

  private companion object {

    val currentTokens = mutableSetOf<KeepAliveToken>()
  }
}
