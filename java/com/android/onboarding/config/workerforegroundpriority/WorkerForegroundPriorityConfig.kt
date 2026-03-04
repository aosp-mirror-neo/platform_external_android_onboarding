package com.android.onboarding.config.workerforegroundpriority

import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.Keep
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistableList
import com.android.onboarding.utils.persistable.putPersistableList
import kotlinx.parcelize.Parcelize

/**
 * A config that controls the priority of foreground activities launched by different workers.
 *
 * @param priorities A list of [WorkerForegroundPriority] objects.
 */
@Parcelize
@Keep
data class WorkerForegroundPriorityConfig(val priorities: List<WorkerForegroundPriority>) :
  Parcelable, Persistable, StableEquality {

  fun getPriority(workerId: Int): Int? = priorities.find { it.workerId == workerId }?.priority

  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().apply { putPersistableList(KEY_PRIORITIES, priorities) }

  companion object : PersistableDeserializer<WorkerForegroundPriorityConfig> {
    internal const val KEY_PRIORITIES = "priorities"

    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): WorkerForegroundPriorityConfig =
      WorkerForegroundPriorityConfig(
        priorities =
          persistableBundle
            .getPersistableList(KEY_PRIORITIES, WorkerForegroundPriority)
            ?.filterNotNull() ?: emptyList()
      )

    /**
     * Merge this config with a parent config. Child config wins for matching workerIds.
     *
     * Returns the merged config.
     */
    fun merge(
      childConfig: WorkerForegroundPriorityConfig?,
      parentConfig: WorkerForegroundPriorityConfig?,
    ): WorkerForegroundPriorityConfig? {
      if (childConfig == null && parentConfig == null) {
        return null
      }
      if (parentConfig == null) {
        return childConfig
      }
      if (childConfig == null) {
        return parentConfig
      }

      val parentMap = parentConfig.priorities.associateBy { it.workerId }
      val childMap = childConfig.priorities.associateBy { it.workerId }

      val mergedMap = parentMap + childMap // Child entries overwrite parent entries

      return WorkerForegroundPriorityConfig(mergedMap.values.toList())
    }
  }
}
