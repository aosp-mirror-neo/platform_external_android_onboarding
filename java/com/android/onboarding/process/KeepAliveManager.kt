package com.android.onboarding.process

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.DeadObjectException
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.Process
import androidx.annotation.VisibleForTesting
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.safeBindService
import com.android.onboarding.utils.safeUnbindService
import com.android.onboarding.versions.DefaultOnboardingChanges
import com.android.onboarding.versions.OnboardingChanges
import com.android.onboarding.versions.changes.KEEP_ALIVE_CHANGE_ID
import java.util.function.Supplier

/**
 * Class that protects the calling process from system termination by binding [serviceClassName] to
 * the onboarding controller [controllerPackageName].
 *
 * @throws [IllegalArgumentException] if [controllerPackageName] or [serviceClassName] is empty.
 */
open class KeepAliveManager(
  contextProvider: Supplier<Context>,
  private val controllerPackageName: String,
  private val serviceClassName: String,
  private val onboardingChanges: Lazy<OnboardingChanges> = lazy {
    DefaultOnboardingChanges(contextProvider.get())
  },
) {
  init {
    if (controllerPackageName.isEmpty() || serviceClassName.isEmpty()) {
      throw IllegalArgumentException("Empty controller package name or service class name.")
    }
  }

  private lateinit var messenger: Messenger
  private val applicationContext: Context by lazy { contextProvider.get().applicationContext }

  /** Connection between the task runner service and the controller service. */
  private val connection =
    object : ServiceConnection {

      override fun onServiceConnected(className: ComponentName, service: IBinder) {
        if (currentTokens.isEmpty()) {
          // Race condition: Service is only connected after client requested unbind. Controller has
          // not proactively requested binding to the client yet. We will unbind from the controller
          // here in this case.
          Log.w(
            TAG,
            "[KeepAlive] Race condition: ${applicationContext.packageName} is connected to: $className after requesting unbind. Unbinding.",
          )
          safeUnbindService(applicationContext)
          return
        }
        // This is called when the connection with the service has been established, giving us the
        // object we can use to interact with the service.
        Log.d(
          TAG,
          "[KeepAlive] Client ${applicationContext.packageName} is connected to: $className.",
        )

        messenger = Messenger(service)

        requestBindFromOnboardingControllerService()
      }

      override fun onServiceDisconnected(className: ComponentName) {
        // This is called when the connection with the service has been unexpectedly disconnected.
        Log.w(
          TAG,
          "[KeepAlive] Client ${applicationContext.packageName} is unexpectedly disconnected from: $className",
        )
      }

      private fun requestBindFromOnboardingControllerService() {
        val msg = Message.obtain(/* handler= */ null, /* what= */ MSG_BIND_ONBOARDING_CONTROLLER)
        msg.data =
          Bundle().apply {
            putString(EXTRA_PACKAGE, applicationContext.packageName)
            putString(EXTRA_COMPONENT, serviceClassName)
            putInt(Intent.EXTRA_UID, Process.myUid())
          }
        Log.d(
          TAG,
          "[KeepAlive] Client ${applicationContext.packageName} with UID ${Process.myUid()} requesting bind from controller.",
        )
        messenger.send(msg)
      }
    }

  /**
   * API used to keep the calling process alive by binding [serviceClassName] with the
   * [controllerPackageName].
   *
   * Accepts a [reason] explaining why the calling process should remain alive. Returns an
   * [AutoCloseable], allowing for both lambda parameters and termination using the close API.
   *
   * Returns an empty [AutoCloseable] if the caller is the [controllerPackageName], or if arguments
   * for service binding are missing, or if the service binding fails.
   *
   * @throws [IllegalArgumentException] if [controllerPackageName] is the caller itself.
   */
  open fun keepAlive(reason: KeepAliveReason): AutoCloseable {
    if (!onboardingChanges.value.currentProcessSupportsChange(KEEP_ALIVE_CHANGE_ID)) {
      // Feature is disabled. Return an [AutoCloseable] that does nothing on [close].
      Log.d(TAG, "[KeepAlive] Feature is disabled.")
      return AutoCloseable {}
    }
    if (applicationContext.packageName.equals(controllerPackageName)) {
      throw IllegalArgumentException("Invalid keep alive request from the controller app itself.")
    }
    val keepAliveToken = KeepAliveToken()
    // We need to add to the collection here because it is used for race condition detection.
    currentTokens.add(keepAliveToken)
    Log.i(
      TAG,
      "[KeepAlive] KeepAlive request from ${applicationContext.packageName} with $reason. Request count: ${currentTokens.size}",
    )
    if (currentTokens.size == 1) {
      // Only bind service if it's the first request from this app.
      if (bindController()) {
        return keepAliveToken
      } else {
        // Don't return a KeepAliveToken if service binding fails.
        currentTokens.remove(keepAliveToken)
        return AutoCloseable {}
      }
    } else {
      // Keep track of KeepAlive attempts from the same app.
      return keepAliveToken
    }
  }

  /**
   * Binds to the controller in order to keep the calling app alive.
   *
   * Returns `true` if the binding is successful.
   */
  private fun bindController(): Boolean {
    val bindIntent = Intent(ACTION_BIND_ONBOARDING_CONTROLLER)
    bindIntent.component = ComponentName(controllerPackageName, ONBOARDING_CONTROLLER_CLASS)
    val bound = connection.safeBindService(applicationContext, bindIntent)
    if (bound) {
      Log.i(
        TAG,
        "[KeepAlive] Client ${applicationContext.packageName} with UID ${Process.myUid()} is bound to ${bindIntent.component}",
      )
    }
    return bound
  }

  private fun unbindController() {
    Log.d(
      TAG,
      "[KeepAlive] Client ${applicationContext.packageName} with UID ${Process.myUid()} requesting unbind from controller.",
    )
    if (!this::messenger.isInitialized) {
      Log.w(
        TAG,
        "[KeepAlive] Race condition: messenger is not initialized when requested to unbind.",
      )
      // Race condition: this can be called before the async onServiceConnected. Controller has not
      // proactively requested binding to the client yet. The client will unbind from the controller
      // in onServiceConnected in this case.
      return
    }

    val msg = Message.obtain(/* handler= */ null, /* what= */ MSG_UNBIND_ONBOARDING_CONTROLLER)
    msg.data =
      Bundle().apply {
        putString(EXTRA_PACKAGE, applicationContext.packageName)
        putInt(Intent.EXTRA_UID, Process.myUid())
      }
    try {
      messenger.send(msg)
      connection.safeUnbindService(applicationContext)
    } catch (e: DeadObjectException) {
      Log.w(TAG, "[KeepAlive] Connection with controller died. No need to unbind anymore.")
    }
  }

  /** This token is used by the calling process to terminate the KeepAlive Service API. */
  inner class KeepAliveToken : AutoCloseable {

    override fun close() {
      currentTokens.remove(this)
      Log.d(
        TAG,
        "[KeepAlive] Client ${applicationContext.packageName} requested to close KeepAliveToken. Tokens size: ${currentTokens.size}",
      )
      if (currentTokens.isEmpty()) {
        unbindController()
      }
    }
  }

  companion object {
    /**
     * A set of [KeepAliveToken] that represents the keep alive request counts.
     *
     * This can be used to decide whether to bind or unbind the controller service.
     */
    val currentTokens = mutableSetOf<KeepAliveToken>()

    private const val TAG = "KeepAliveManager"

    /**
     * The process on which currently the onboarding controller service depends on to keep alive the
     * onboarding app clients while doing some background work.
     */
    @VisibleForTesting
    const val ONBOARDING_CONTROLLER_CLASS =
      "com.android.onboarding.process.onboardinghost.KeepAliveControllerService"
    const val EXTRA_PACKAGE = "package"
    const val EXTRA_COMPONENT = "component"
    /** Action to establish a binding to [ONBOARDING_CONTROLLER_CLASS]. */
    const val ACTION_BIND_ONBOARDING_CONTROLLER =
      "com.android.onboarding.process.onboardinghost.ACTION_BIND_ONBOARDING_CONTROLLER"
    /**
     * Message requesting the service to establish a binding back to the client to keep them alive.
     */
    const val MSG_BIND_ONBOARDING_CONTROLLER = 1
    /**
     * Message requesting the service to unbind from the client, indicating that the client is no
     * longer needed to keep the service alive.
     */
    const val MSG_UNBIND_ONBOARDING_CONTROLLER = 2
  }
}

/**
 * Enum representing reasons for keeping an onboarding process alive. These reasons are used to
 * determine when to utilize the Keep Alive API.
 */
enum class KeepAliveReason {

  // [KeepAliveReason]s to this
  // enum.
  TEST_ONLY, // For testing purposes

  // only from
  // test only targets.
  OTHER, // Other unspecified reasons
}
