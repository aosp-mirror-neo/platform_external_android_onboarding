package com.android.onboarding.nodes

import java.time.Instant

/**
 * Basic [IOnboardingGraph] representation with data available from a set of
 * [OnboardingGraphLog.OnboardingEventDelegate] only.
 */
typealias OnboardingGraph =
  IOnboardingGraph<OnboardingGraphLog.OnboardingEventDelegate, OnboardingGraphNode>

/** A core representation of the onboarding graph mainly built from [TEvent]s. */
interface IOnboardingGraph<
  out TEvent : OnboardingGraphLog.OnboardingEventDelegate,
  out TNode : IOnboardingGraphNode<TEvent>,
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
     * @param events [IOnboardingGraph.events]
     */
    operator fun invoke(
      events: Iterable<OnboardingGraphLog.OnboardingEventDelegate>
    ): OnboardingGraph = OnboardingGraphBuilder.build(events)
  }
}
