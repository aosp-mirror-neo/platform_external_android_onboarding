package com.android.onboarding.tasks

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
@Deprecated("We will remove this class very soon after we fix the build error in cl/619298504.")
class DefaultOnboardingTaskManager
private constructor(appContext: Context, coroutineScope: CoroutineScope) :
  AbstractOnboardingTaskManager(appContext, coroutineScope) {

  override val componentName = "DefaultOnboardingTaskManager"

  override fun initializeContractAndTaskMap() =
    ConcurrentHashMap<Class<out OnboardingTaskContract<*, *>>, Class<out OnboardingTask<*, *, *>>>()

  /**
   * Gets an instance of [DefaultOnboardingTaskManager], creating one if it does not exist.
   *
   * @return The singleton instance of [DefaultOnboardingTaskManager].
   */
  companion object {

    const val TAG: String = "DefaultOTM"
    private var instance: DefaultOnboardingTaskManager? = null

    /**
     * Gets an instance of [DefaultOnboardingTaskManager], creating one if it does not exist. A
     * default coroutine scope with [Dispatchers.Default] and [SupervisorJob] will be used for this
     * function.
     *
     * @param appContext The application context.
     * @return The singleton instance of [DefaultOnboardingTaskManager].
     */
    fun getInstance(appContext: Context): DefaultOnboardingTaskManager {
      return getInstance(appContext, CoroutineScope(Dispatchers.Default + SupervisorJob()))
    }

    /**
     * Gets an instance of [DefaultOnboardingTaskManager], creating one if it does not exist. You
     * can customize the coroutine scope by providing your own [CoroutineScope] instance. If not
     * provided, a default coroutine scope with [Dispatchers.Default] and [SupervisorJob] will be
     * used.
     *
     * @param appContext The application context.
     * @param coroutineScope The optional [CoroutineScope] for custom coroutine handling.
     * @return The singleton instance of [DefaultOnboardingTaskManager].
     */
    fun getInstance(
      appContext: Context,
      coroutineScope: CoroutineScope? = null,
    ): DefaultOnboardingTaskManager {
      return instance
        ?: synchronized(this) {
          instance
            ?: DefaultOnboardingTaskManager(
                appContext,
                coroutineScope ?: CoroutineScope(Dispatchers.Default + SupervisorJob()),
              )
              .also { instance = it }
        }
    }

    fun release() {
      instance?.coroutineScope?.cancel()
      instance = null
    }
  }
}
