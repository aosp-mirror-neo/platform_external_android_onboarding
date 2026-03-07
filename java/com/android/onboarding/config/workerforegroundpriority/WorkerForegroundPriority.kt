package com.android.onboarding.config.workerforegroundpriority

import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.Keep
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.NullSafePersistableBundleExtensions.getIntOrThrow
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import kotlinx.parcelize.Parcelize

/**
 * Represents the foreground priority for a single worker.
 *
 * @param workerId The ID of the worker.
 * @param priority The foreground priority value. Higher values mean higher priority. -1 means the
 *   worker is not allowed to launch foreground activities.
 */
@Parcelize
@Keep
data class WorkerForegroundPriority(val workerId: Int, val priority: Int) :
  Parcelable, Persistable, StableEquality {
  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().apply {
      putInt(KEY_WORKER_ID, workerId)
      putInt(KEY_PRIORITY, priority)
    }

  companion object : PersistableDeserializer<WorkerForegroundPriority> {
    internal const val KEY_WORKER_ID = "workerId"
    internal const val KEY_PRIORITY = "priority"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      WorkerForegroundPriority(
        workerId = persistableBundle.getIntOrThrow(KEY_WORKER_ID),
        priority = persistableBundle.getIntOrThrow(KEY_PRIORITY),
      )
  }
}
