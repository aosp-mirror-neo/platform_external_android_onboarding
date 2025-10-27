package com.android.onboarding.control.annotations

import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.control.OrchestratedResult
import com.android.onboarding.tasks.OnboardingTaskContract
import com.android.onboarding.utils.persistable.PersistableIntent
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotation
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotation.ExtractMetadata
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotationCompanionDelegate
import kotlin.reflect.KClass

/**
 * Annotation applied to an Onboarding Activity API Contract to indicate that the work performed by
 * the activity can (at least in some circumstances) be alternatively performed by a background
 * task.
 *
 * The background task must accept a PersistableIntent as the argument and it must produce an
 * orchestrated result. It will be executed with the intent produced by the contract.
 *
 * This can be used in cases where the entire functionality of the activity contract can be
 * performed by the background task, in cases where some paths can but in others we need the UI (
 * which can be triggered using the orchestrated result), and in cases where we can perform some
 * pre-processing that can skip the activity in some cases but otherwise we use the UI for the
 * entire execution.
 */
@PersistableAnnotation(lossy = true)
annotation class HasTaskAlternative(
  @get:ExtractMetadata(OnboardingNode::class)
  val task: KClass<out OnboardingTaskContract<PersistableIntent, out OrchestratedResult<*>>>
) {
  companion object :
    PersistableAnnotationCompanionDelegate<HasTaskAlternative, PHasTaskAlternative>(
      PHasTaskAlternative
    )
}
