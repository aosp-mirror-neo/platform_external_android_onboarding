package com.android.onboarding.nodes

import java.time.Duration
import java.time.Instant

/** A node in the [OnboardingGraph]. */
interface OnboardingGraphNode {
  /** The node's UUID. */
  val id: Long

  /** The name of this node. */
  val name: String
  val argument: Any?
  val result: Any?

  /** The time this node entered the graph. */
  val start: Instant

  /** The time this node exited the graph. */
  val end: Instant

  /** All the graph edges this node has initiated. */
  val outgoingEdges: Set<OnboardingGraphEdge.Outgoing>
  val outgoingEdgesOfValidNodes: Set<OnboardingGraphEdge.Outgoing>

  /** The graph edge this node has received. */
  val incomingEdge: OnboardingGraphEdge.Incoming?
  val failureReasons: Set<Throwable>
  val isFailed: Boolean
  val type: OnboardingGraphNode.Type

  /** The name of the onboarding component that owns this node. */
  val component: String?
  val isSynchronous: Boolean

  /** All the raw onboarding events related to this node. */
  val events: Set<OnboardingGraphLog.OnboardingEvent>
  val isComplete: Boolean

  /**
   * The time between node's [start] and [end]. Includes the time this node spent waiting or in the
   * background.
   */
  val totalRuntime: Duration

  /** The actual time this node was active. */
  val individualRuntime: Duration

  enum class Type {
    UNKNOWN,
    ACTIVITY,
    SYNCHRONOUS,
  }
}
