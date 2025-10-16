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
  vararg val triggers: PrecalculationTrigger,
  val readOnlyOnce: Boolean = false,
  val runAgainOnSuccessiveTriggers: Boolean = false,
) {
  companion object :
    PersistableAnnotationCompanionDelegate<Precalculation, PPrecalculation>(PPrecalculation)
}
