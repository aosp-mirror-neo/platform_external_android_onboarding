package com.android.onboarding.tasks

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.android.onboarding.utils.logging.OnboardingLog
import java.lang.ref.WeakReference

/**
 * Android [Lifecycle] manager implementation used by [OnboardingTasks][OnboardingTask].
 *
 * Based on [androidx.lifecycle.ServiceLifecycleDispatcher]
 */
internal class OnboardingTaskLifecycle(provider: LifecycleOwner) : Lifecycle() {
  private val registry = LifecycleRegistry(provider)
  private val lifecycleOwner = WeakReference(provider)
  private val handler: Handler = Handler(Looper.getMainLooper())

  private fun postRegistryRunnable(description: String, action: (LifecycleRegistry) -> Unit) {
    if (!RegistryRunnable(registry, lifecycleOwner, action).let(handler::post)) {
      log.w("Failed to post registry runnable: $description")
    }
  }

  override val currentState: State
    get() = registry.currentState

  override fun addObserver(observer: LifecycleObserver) {
    postRegistryRunnable("addObserver") { it.addObserver(observer) }
  }

  override fun removeObserver(observer: LifecycleObserver) {
    postRegistryRunnable("removeObserver") { it.removeObserver(observer) }
  }

  /** Notify the lifecycle of a new lifecycle [event]. */
  internal fun handleLifecycleEvent(event: Event) {
    postRegistryRunnable("handleLifecycleEvent($event)") { it.handleLifecycleEvent(event) }
  }

  private class RegistryRunnable(
    private val registry: LifecycleRegistry,
    private val lifecycleOwner: WeakReference<LifecycleOwner>,
    private val action: (LifecycleRegistry) -> Unit,
  ) : Runnable {
    private var wasExecuted = false

    override fun run() {
      if (!wasExecuted) {
        if (lifecycleOwner.get() != null) action(registry)
        wasExecuted = true
      }
    }
  }

  private companion object {
    val log = OnboardingLog.logger("OnboardingTaskLifecycle")
  }
}
