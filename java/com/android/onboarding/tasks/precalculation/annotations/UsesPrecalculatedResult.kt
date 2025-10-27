package com.android.onboarding.tasks.precalculation.annotations

import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.tasks.precalculation.PrecalculationTaskContract
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotation
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotation.ExtractMetadata
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotationCompanionDelegate
import kotlin.reflect.KClass

/**
 * Annotates an Activity contract to indicate that it uses the precalculated results from the given
 * tasks.
 *
 * This annotation should only be applied to Activity Api Contracts which accept an Intent as the
 * argument.
 */
@PersistableAnnotation(lossy = true)
annotation class UsesPrecalculatedResult(vararg val results: PrecalculatedResult) {
  companion object :
    PersistableAnnotationCompanionDelegate<UsesPrecalculatedResult, PUsesPrecalculatedResult>(
      PUsesPrecalculatedResult
    )
}

/**
 * Mapping between a task which produces a result and the key which should be used to store that
 * result in the extras of the intent.
 *
 * This extra will be set when a compliant caller executes the contract. The key will not be set if
 * the caller is not precalculating data, or if the precalculated data is not ready at the time of
 * the launch. **Activities must continue to work even if this data is not provided.**
 *
 * @property task The task which produced this precalculated result.
 */
@PersistableAnnotation(lossy = true)
annotation class PrecalculatedResult(
  @get:ExtractMetadata(OnboardingNode::class, Precalculation::class)
  val task: KClass<out PrecalculationTaskContract>,
  val extraKey: String,
) {
  companion object :
    PersistableAnnotationCompanionDelegate<PrecalculatedResult, PPrecalculatedResult>(
      PPrecalculatedResult
    )
}
