package com.android.onboarding.config

import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.Keep
import com.android.onboarding.config.workerforegroundpriority.WorkerForegroundPriorityConfig
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import kotlinx.parcelize.Parcelize

/** A container for various onboarding related configurations. */
@Parcelize
@Keep
data class OnboardingConfig(
  val waitingScreenConfig: WaitingScreenConfig? = null,
  val workerForegroundPriorityConfig: WorkerForegroundPriorityConfig? = null,
) : Parcelable, Persistable, StableEquality {
  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().apply {
      putPersistable(KEY_WAITING_SCREEN_CONFIG, waitingScreenConfig)
      putPersistable(KEY_WORKER_FOREGROUND_PRIORITY_CONFIG, workerForegroundPriorityConfig)
    }

  companion object : PersistableDeserializer<OnboardingConfig> {
    const val KEY_WAITING_SCREEN_CONFIG = "waitingScreenConfig"
    const val KEY_WORKER_FOREGROUND_PRIORITY_CONFIG = "workerForegroundPriorityConfig"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): OnboardingConfig =
      OnboardingConfig(
        waitingScreenConfig =
          persistableBundle.getPersistable(KEY_WAITING_SCREEN_CONFIG, WaitingScreenConfig),
        workerForegroundPriorityConfig =
          persistableBundle.getPersistable(
            KEY_WORKER_FOREGROUND_PRIORITY_CONFIG,
            WorkerForegroundPriorityConfig,
          ),
      )

    /**
     * Merge this config with a parent config.
     *
     * Returns the merged config.
     */
    fun merge(childConfig: OnboardingConfig?, parentConfig: OnboardingConfig?): OnboardingConfig? {
      if (childConfig == null && parentConfig == null) {
        return null
      }
      if (parentConfig == null) {
        // Nothing to override.
        return childConfig
      }
      if (childConfig == null) {
        // Override everything with the parent config.
        return parentConfig
      }
      if (childConfig == OnboardingConfig()) {
        // Child clears config.
        return OnboardingConfig()
      }
      val mergedWaitingScreenConfig =
        WaitingScreenConfig.merge(childConfig.waitingScreenConfig, parentConfig.waitingScreenConfig)
      val mergedWorkerForegroundPriorityConfig =
        WorkerForegroundPriorityConfig.merge(
          childConfig.workerForegroundPriorityConfig,
          parentConfig.workerForegroundPriorityConfig,
        )
      return OnboardingConfig(mergedWaitingScreenConfig, mergedWorkerForegroundPriorityConfig)
    }
  }
}
