package com.android.onboarding.nodes

import java.time.Duration
import java.time.Instant

/**
 * Basic [IOnboardingGraphNode] representation with data available from a set of
 * [OnboardingGraphLog.OnboardingEventDelegate] only.
 */
typealias OnboardingGraphNode = IOnboardingGraphNode<OnboardingGraphLog.OnboardingEventDelegate>

/** Common data for all onboarding node implementations. */
interface OnboardingGraphNodeData {
  /** The node's UUID. */
  val id: Long

  /** The name of this node. */
  val name: String

  /**
   * Fully qualified name of this entity representing its kind. Expected to be distinct from other
   * node kinds within the graph.
   */
  val qualifiedName: String
    get() = "$component/$name"

  val argument: Any?
  val result: Any?

  /** The time this node entered the graph. */
  val start: Instant

  /** The time this node exited the graph. */
  val end: Instant

  /** All the graph edges this node has initiated. */
  val outgoingEdges: Collection<OnboardingGraphEdge.Outgoing>

  /**
   * Filtered subset of [outgoingEdges] without the invalid outgoing edges. i.e. edges to non
   * existent nodes.
   */
  val outgoingEdgesOfValidNodes: Collection<OnboardingGraphEdge.Outgoing>

  /** The graph edge this node has received. */
  val incomingEdge: OnboardingGraphEdge.Incoming?
  val failureReasons: Collection<Throwable>

  /** A list of issue descriptions detected for this node. */
  val issues: Collection<String>

  val isFailed: Boolean
    get() = failureReasons.isNotEmpty()

  /** The node's type. */
  val type: Type

  /** The name of the onboarding component that owns this node. */
  val component: String?
  val isSynchronous: Boolean
    get() = type == Type.SYNCHRONOUS

  val isComplete: Boolean

  /** Is the node unknown (no known ID supplied)? */
  val unknown: Boolean

  /**
   * The time between node's [start] and [end]. Includes the time this node spent waiting or in the
   * background.
   */
  val totalRuntime: Duration
    get() = Duration.between(start, end)

  /** The time this node spend paused. */
  val pausedRuntime: Duration

  /** The actual time this node was active. */
  val individualRuntime: Duration
    get() = totalRuntime - pausedRuntime

  /** Calculates the % of total graph duration that this node took. */
  fun percentOfGraphDuration(graphDuration: Duration): Double =
    individualRuntime.toNanos().toDouble() / graphDuration.toNanos()

  /** Short string representation of the components uniquely identifying this entity's kind. */
  val identity: String
    get() = "Node(id=$id, qualifiedName=$qualifiedName)"

  enum class Type {
    UNKNOWN,
    ACTIVITY,
    SYNCHRONOUS,
  }
}

/** A node in the [OnboardingGraph]. */
interface IOnboardingGraphNode<out TEvent : OnboardingGraphLog.OnboardingEventDelegate> :
  OnboardingGraphNodeData {
  /** All the raw onboarding events related to this node. */
  val events: Collection<TEvent>
    get() = spawnedEvents + relatedEvents

  /** All events this node has spawned. */
  val spawnedEvents: Collection<TEvent>

  /** All events not spawned by this node, but relating to it. */
  val relatedEvents: Collection<TEvent>

  companion object {
    /** Generate standard name for node with [id] and no name. */
    fun unknown(id: Long) = "Unknown(id=$id)"

    /** Generate standard name for node with [id] and no [component]. */
    fun unknownComponent(id: Long) = "UnknownComponent(nodeId=$id)"
  }
}
