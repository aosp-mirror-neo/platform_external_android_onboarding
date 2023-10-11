package com.android.onboarding.contracts.annotations

/** Container of metadata about a node definition in the onboarding graph */
@Retention(AnnotationRetention.RUNTIME)
annotation class OnboardingNode(
  /** Identifier of the component who owns the node. */
  val component: String,

  /**
   * Identifier of the node interface.
   *
   * Note that this must be at most 40 characters long, otherwise an exception will be thrown.
   */
  val name: String,

  /** True if this is a node which presents a UI to the user. */
  val hasUi: HasUi
) {
  enum class HasUi {
    NO,
    YES,
    YES_TEMPORARILY
  }
}

const val MAX_NODE_NAME_LENGTH: Int = 40
