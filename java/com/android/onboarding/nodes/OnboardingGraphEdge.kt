package com.android.onboarding.nodes

import java.time.Instant

/** An one-directional edge in the [OnboardingGraph]. */
sealed interface OnboardingGraphEdge {

  /** [OnboardingGraphNode] node this edge is linked to. */
  val node: OnboardingGraphNode

  /** The time when this edge was initiated. */
  val timestamp: Instant

  /** An [OnboardingGraphEdge] that was received by the [node]. */
  interface Incoming : OnboardingGraphEdge

  /** An [OnboardingGraphEdge] that was initiated by the [node]. */
  interface Outgoing : OnboardingGraphEdge
}
