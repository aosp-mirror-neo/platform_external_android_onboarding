package com.android.onboarding.nodes

typealias OnboardingGraph = TypedOnboardingGraph<OnboardingGraphNode>

interface TypedOnboardingGraph<out TNode : OnboardingGraphNode> {
  val events: Set<OnboardingGraphLog.OnboardingEvent>

  /** A map of all the nodes in the graph as identified by [OnboardingGraphNode.id] */
  val nodes: Map<Long, TNode>

  companion object {
    /**
     * Constructs default implementation of the [OnboardingGraph]
     *
     * @param events [TypedOnboardingGraph.events]
     */
    operator fun invoke(events: Set<OnboardingGraphLog.OnboardingEvent>): OnboardingGraph =
      DefaultOnboardingGraph(events)
  }
}
