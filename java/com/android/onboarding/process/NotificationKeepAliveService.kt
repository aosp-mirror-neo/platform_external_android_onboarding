package com.android.onboarding.process

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Service utilized to ensure that the calling process remains active and is protected from being
 * terminated by the system on displaying a foreground notification.
 */
class NotificationKeepAliveService : Service() {

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d("KeepAliveService", "NotificationKeepAliveService Started")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannel()
      val notification =
        Notification.Builder(this, "keep_alive_id")
          .setSmallIcon(R.drawable.notification_action_background)
          .setOngoing(true)
          .build()

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
      } else {
        startForeground(NOTIFICATION_ID, notification)
      }
    }

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    Log.d("KeepAliveService", "NotificationKeepAliveService Stopped")
  }

  @TargetApi(Build.VERSION_CODES.O)
  private fun createNotificationChannel() {
    val notificationChannel =
      NotificationChannel("keep_alive_id", "Keep Alive", NotificationManager.IMPORTANCE_MIN)
    val notificationManager = getSystemService(NotificationManager::class.java)
    notificationManager?.createNotificationChannel(notificationChannel)
  }

  private companion object {

    const val NOTIFICATION_ID = 12345
  }
}
