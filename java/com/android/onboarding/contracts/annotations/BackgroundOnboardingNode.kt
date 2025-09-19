package com.android.onboarding.contracts.annotations

import com.android.onboarding.utils.persistable.annotations.PersistableAnnotation
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotationCompanionDelegate

/** Additional [OnboardingNode] annotation data used for nodes without a UI. */
@Retention(AnnotationRetention.RUNTIME)
@Target
@PersistableAnnotation
annotation class BackgroundOnboardingNode(
  /** Interrupts which can be passed to this node. */

  //  interrupts with invalid reason
  val acceptsInterrupts: Array<InterruptReason> = [],

  /** Value used only by infrastructure. This should not be changed by an individual node. */
  val empty: Boolean = false,
) {
  companion object :
    PersistableAnnotationCompanionDelegate<BackgroundOnboardingNode, PBackgroundOnboardingNode>(
      PBackgroundOnboardingNode
    )
}
