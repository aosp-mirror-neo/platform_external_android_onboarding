package com.android.onboarding.nodes.testing.testapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.android.onboarding.process.KeepAliveReason
import com.android.onboarding.process.OnboardingProcessImpl

/**
 * This service is utilized by the Onboarding Graph TestApp to examine the behavior of the Keep
 * Alive APIs. It provides a controlled mechanism to enable or disable process keep-alive
 * functionality, facilitating testing of how the TestApp interacts with these APIs.
 *
 * **Responds to Start Intent:** The service expects a boolean extra named "usingKeepAlive" in its
 * start Intent.
 * * **True:** Instructs the underlying Keep Alive library to actively keep the process alive.
 * * **False:** Directs the Keep Alive library to cease any active keep-alive behavior.
 *
 * **Testable Scenarios**
 * 1. **Keep-Alive Enabled:**
 *     * Start the service with the "usingKeepAlive" extra set to `true`.
 *     * Verify that the TestApp process remains alive under conditions that would typically trigger
 *       termination (e.g., background state, low memory).
 * 2. **Keep-Alive Disabled:**
 *     * Start the service with the "usingKeepAlive" extra set to `false`.
 *     * Verify that the TestApp process behaves normally and is subject to termination under
 *       expected conditions.
 */
class BackgroundTaskService : Service() {

  private var isRunning = false
  private var keepAliveToken: AutoCloseable? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d("BackgroundTaskService", "BackgroundTaskService Started")
    sendBroadcast(createStatusIntent(isRunning = true))

    isRunning = true
    if (intent?.getBooleanExtra("usingKeepAlive", false) == true) {
      keepAliveToken =
        OnboardingProcessImpl(applicationContext).keepAlive(reason = KeepAliveReason.TEST_ONLY)
    }

    Thread {
        while (isRunning) {
          Log.d("BackgroundTaskService", "BackgroundTaskService is running...")
          Thread.sleep(5000)
        }
      }
      .start()

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    sendBroadcast(createStatusIntent(isRunning = false))

    isRunning = false
    keepAliveToken?.close()

    Log.d("BackgroundTaskService", "BackgroundTaskService Stopped")
  }

  private fun createStatusIntent(isRunning: Boolean): Intent {
    Log.d("BackgroundTaskService", "Service running status broadcast as $isRunning")

    return Intent(BROADCAST_ACTION).putExtra(SERVICE_STATUS, isRunning)
  }

  companion object {
    // Action for broadcasting the current running status of the [BackgroundTaskService].
    const val BROADCAST_ACTION =
      "com.android.onboarding.nodes.testing.testapp.SERVICE_STATUS_UPDATE"
    // Intent extra for storing the boolean value indicating the current [BackgroundTaskService]
    // running status.
    const val SERVICE_STATUS = "service_status"
  }
}
