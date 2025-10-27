package com.android.onboarding.tasks.precalculation

import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.tasks.ArgumentFreeOnboardingTaskContract

/**
 * Contract representing a task intended to precalculate data for later use by an activity.
 *
 * This represents the "external" contract which will be referenced by contracts which depend on
 * this precalculated data. The task which implements this precalculation must instead reference a
 * [TypedPrecalculationTaskContract] which can be implemented either by the
 * [PrecalculationTaskContract] subclass or by a different class.
 *
 * This separation allows for the creation of precalculation contracts without needing to expose the
 * implementation details and dependencies of the data calculated by the task.
 */
abstract class PrecalculationTaskContract :
  ArgumentFreeOnboardingTaskContract<PrecalculatedData>() {
  @OptIn(InternalOnboardingApi::class)
  final override fun performEncodeResult(taskResult: PrecalculatedData) =
    taskResult.toPersistableBundle()

  @OptIn(InternalOnboardingApi::class)
  final override fun performExtractResult(bundle: PersistableBundle) =
    PrecalculatedData.fromPersistableBundle(bundle)
}
