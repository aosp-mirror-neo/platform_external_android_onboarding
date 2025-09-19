package com.android.onboarding.contracts.annotations

import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer

// This is in the annotations package just for visibility reasons - so we don't need annotations
// to depend on the full tasks package - plus it's relatively simple

/** Reasons that an Onboarding Task has been interrupted. */
enum class InterruptReason(private val type: Int) : Persistable, StableEquality {
  /** The user has chosen to cancel execution of this task. */
  USER_CANCELLED(1),
  /**
   * The result of this task is no longer needed and therefore the task should be cancelled.
   *
   * Consider if allowing the task to complete would be more appropriate. Interrupting the task can
   * leave any modified state in an unpredictable state.
   */
  RESULT_NO_LONGER_NEEDED(2),

  /** The interrupt reason was not recognised. */
  UNKNOWN(-1);

  override fun toPersistableBundle() = PersistableBundle().apply { putInt(KEY_TYPE, type) }

  companion object : PersistableDeserializer<InterruptReason> {

    @VisibleForTesting internal const val KEY_TYPE = "type"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): InterruptReason {
      val type = persistableBundle.getInt(KEY_TYPE, UNKNOWN.type)

      return entries.firstOrNull { it.type == type } ?: UNKNOWN
    }
  }
}
