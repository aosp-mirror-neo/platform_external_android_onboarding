package com.android.onboarding.nodes

import java.time.Instant

typealias OnboardingGraph = TypedOnboardingGraph<OnboardingGraphNode>

interface TypedOnboardingGraph<out TNode : OnboardingGraphNode> {
  /**
   * All events related to this graph, sorted by [OnboardingGraphLog.OnboardingEventData.timestamp]
   */
  val events: Iterable<OnboardingGraphLog.OnboardingEvent>

  /** A map of all the nodes in the graph as identified by [OnboardingGraphNode.id] */
  val nodes: Map<Long, TNode>

  /** The time when the first node entered the graph. */
  val start: Instant
    get() = events.firstOrNull()?.timestamp ?: Instant.now()

  /** The time when the last node exited the graph. */
  val end: Instant
    get() = events.lastOrNull()?.timestamp ?: start

  companion object {
    /**
     * Constructs default implementation of the [OnboardingGraph]
     *
     * @param events [TypedOnboardingGraph.events]
     */
    operator fun invoke(events: Iterable<OnboardingGraphLog.OnboardingEvent>): OnboardingGraph =
      DefaultOnboardingGraph(
        events.distinct().sortedBy(OnboardingGraphLog.OnboardingEventData::timestamp)
      )
  }
}
