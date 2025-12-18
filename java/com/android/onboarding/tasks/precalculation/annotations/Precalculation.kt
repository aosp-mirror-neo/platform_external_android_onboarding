package com.android.onboarding.tasks.precalculation.annotations

import com.android.onboarding.tasks.precalculation.PrecalculationTrigger
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotation
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotationCompanionDelegate

/**
 * Annotates a
 * [PrecalculationTaskContract][com.android.onboarding.tasks.precalculation.PrecalculationTaskContract]
 * with the triggers (see [PrecalculationTrigger]) for when the task should be run.
 */
@PersistableAnnotation
annotation class Precalculation(
  /** The triggers that will start this precalculation task. */
  vararg val triggers: PrecalculationTrigger,
  /**
   * Whether the result of this precalculation is read-once. If true, the result will be removed
   * from the cache after it is read.
   */
  val readOnlyOnce: Boolean = false,
  /**
   * Whether the task should run again on subsequent triggers. If true, the task will be re-executed
   * if it is triggered again, even if it has already run.
   */
  val runAgainOnSuccessiveTriggers: Boolean = false,
  /**
   * Whether all triggers are required to start the task. If true, the task will only start when all
   * [triggers] have been fired. If false, the task will start when any of the [triggers] are fired.
   */
  val requireAllTriggers: Boolean = false,
) {
  companion object :
    PersistableAnnotationCompanionDelegate<Precalculation, PPrecalculation>(PPrecalculation)
}
