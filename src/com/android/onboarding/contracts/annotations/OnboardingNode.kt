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
  val hasUi: HasUi,

  /** The type of specification given for this node. */
  val specificationType: SpecificationType = SpecificationType.LIGHT
) {
  enum class HasUi {
    NO,
    YES,
    YES_TEMPORARILY
  }

  enum class SpecificationType {
    /**
     * This means that full Javadoc is provided, all arguments are fully defined, documented and
     * typed (no undefined Bundles or Intents), and return values are properly defined and typed.
     */
    V1,

    /**
     * No requirements.
     *
     * Most "Light" nodes will be lacking Javadoc, and using Intents and Bundles as arguments and
     * return values.
     */
    LIGHT
  }
}

const val MAX_NODE_NAME_LENGTH: Int = 40
