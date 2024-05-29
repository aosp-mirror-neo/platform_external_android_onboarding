package com.android.onboarding.nodes

import java.time.Instant

/**
 * Basic [TypedOnboardingGraph] representation with data available from a set of
 * [OnboardingGraphLog.OnboardingEventDelegate] only.
 */
typealias OnboardingGraph =
  TypedOnboardingGraph<OnboardingGraphNode, OnboardingGraphLog.OnboardingEventDelegate>

/** A core representation of the onboarding graph mainly built from [TEvent]s. */
interface TypedOnboardingGraph<
  out TNode : OnboardingGraphNode,
  out TEvent : OnboardingGraphLog.OnboardingEventDelegate,
> {
  /**
   * All events related to this graph, sorted by [OnboardingGraphLog.OnboardingEventData.timestamp]
   */
  val events: Iterable<TEvent>

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
    operator fun invoke(
      events: Iterable<OnboardingGraphLog.OnboardingEventDelegate>
    ): OnboardingGraph =
      DefaultOnboardingGraph(
        events.distinct().sortedBy(OnboardingGraphLog.OnboardingEventData::timestamp)
      )
  }
}
