package com.android.onboarding.nodes

import java.time.Duration
import java.time.Instant
import java.util.Objects

class DefaultOnboardingGraph(override val events: Set<OnboardingGraphLog.OnboardingEvent>) :
  OnboardingGraph {
  override val nodes: Map<Long, NodeImpl> by lazy {
    val nodeMap = mutableMapOf<Long, NodeImpl>()

    for (e in events) {
      when (e) {
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeExecutedDirectly -> {
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            argument = e.argument,
            incomingEdge = InternalEdge.ClosedIncomingEdge(e.sourceNodeId, e.timestamp),
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
          nodeMap.updateNode(
            e,
            e.sourceNodeId,
            e.timestamp,
            outgoingEdge = InternalEdge.OutgoingEdge(e.nodeId, e.timestamp),
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeExecutedForResult -> {
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            argument = e.argument,
            incomingEdge = InternalEdge.OpenIncomingEdge(e.sourceNodeId, e.timestamp),
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
          nodeMap.updateNode(
            e,
            e.sourceNodeId,
            e.timestamp,
            outgoingEdge = InternalEdge.OutgoingEdge(e.nodeId, e.timestamp),
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeValidating -> {
          // We don't log the incoming intent beyond the normal event log as it's an implementation
          // detail
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeExtractArgument -> {
          // We don't log the incoming intent beyond the normal event log as it's an implementation
          // detail
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeFailedValidation -> {
          // We don't log the incoming intent beyond the normal event log as it's an implementation
          // detail
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            failureReason = e.exception,
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeArgumentExtracted -> {
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            argument = e.argument,
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeSetResult -> {
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            result = e.result,
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeResultReceived -> {
          // Will be dealt with in the second loop
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeFail -> {
          // Will be dealt with in the second loop
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeStartExecuteSynchronously -> {
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            argument = e.argument,
            incomingEdge = InternalEdge.OpenIncomingEdge(e.sourceNodeId, e.timestamp),
            type = OnboardingGraphNode.Type.SYNCHRONOUS,
          )
          nodeMap.updateNode(
            e,
            e.sourceNodeId,
            e.timestamp,
            outgoingEdge = InternalEdge.OutgoingEdge(e.nodeId, e.timestamp),
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeExecutedSynchronously -> {
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            result = e.result,
            type = OnboardingGraphNode.Type.SYNCHRONOUS,
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeFinished -> {
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeResumedAfterLaunch -> {
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
        }
        is OnboardingGraphLog.OnboardingEvent.ActivityNodeResumed -> {
          nodeMap.updateNode(
            e,
            e.nodeId,
            e.timestamp,
            name = e.nodeName,
            component = e.nodeComponent,
            type = OnboardingGraphNode.Type.ACTIVITY,
          )
        }
      }
    }

    // We run through a second time to update results now that all nodes exist
    for (e in events) {
      if (e is OnboardingGraphLog.OnboardingEvent.ActivityNodeResultReceived) {
        nodeMap.updateNode(
          e,
          e.nodeId,
          e.timestamp,
          name = e.nodeName,
          component = e.nodeComponent,
          result = e.result,
        )
        val node = nodeMap[e.nodeId]!!
        node._incomingEdge?.let { nodeMap.updateNode(id = it.id, timestamp = e.timestamp) }
      } else if (e is OnboardingGraphLog.OnboardingEvent.ActivityNodeFail) {
        nodeMap.updateNode(
          e,
          e.nodeId,
          e.timestamp,
          failureReason = IllegalStateException(e.reason),
        )
        val node = nodeMap[e.nodeId]!!
        with(node._incomingEdge) {
          if (this == null) {
            println("node._incomingEdge is null. Event: $e, Node: $node")
          } else {
            nodeMap.updateNode(id = this.id, timestamp = e.timestamp)
          }
        }
      }
    }

    val nodesToRemove = mutableListOf<Long>()

    // Then we need to recursively expand the time of all callers which are waiting for a result -
    // as they haven't finished executing
    for (node in nodeMap.values) {
      var recursiveNode: NodeImpl? = node
      while (recursiveNode != null) {
        recursiveNode =
          recursiveNode._incomingEdge?.let { incomingEdge ->
            if (incomingEdge is InternalEdge.OpenIncomingEdge) {
              // If the edge is "Open" then it's waiting for a reply to so the incoming node must
              // be at least as long as this node
              val incomingNode = nodeMap[incomingEdge.id]!!
              node.start
                .takeIf { it.isBefore(incomingNode.start) }
                ?.also { incomingNode._start = it }
              node.end.takeIf { it.isAfter(incomingNode.end) }?.also { incomingNode._end = it }

              incomingNode
            } else null
          }
      }

      if (node.isSynchronous) {
        if (node._events.size == 1) {
          // It only started and did nothing else
          nodesToRemove.add(node.id)
        }
      }
    }

    nodesToRemove.forEach { nodeMap.remove(it) }

    nodeMap.toMap()
  }

  private fun MutableMap<Long, NodeImpl>.updateNode(
    event: OnboardingGraphLog.OnboardingEvent? = null,
    id: Long,
    timestamp: Instant,
    name: String? = null,
    component: String? = null,
    argument: Any? = null,
    result: Any? = null,
    outgoingEdge: InternalEdge? = null,
    incomingEdge: InternalEdge? = null,
    failureReason: Throwable? = null,
    type: OnboardingGraphNode.Type? = null,
  ) {
    this.getOrPut(id) { NodeImpl(this, id) }
      .also { node ->
        name?.also { node._name = it }
        component?.also { node._component = it }

        argument?.also { node._argument = it }
        result?.also { node._result = it }

        if (node._start == null || node._start?.let(timestamp::isBefore) == true) {
          node._start = timestamp
        }
        if (node._end == null || node._end?.let(timestamp::isAfter) == true) {
          node._end = timestamp
        }

        failureReason?.also { node._failureReasons.add(it) }

        outgoingEdge?.also {
          node._outgoingEdges
            .getOrPut(it.id) { it }
            .let { created ->
              created.timestamp =
                if (it.timestamp.isBefore(created.timestamp)) it.timestamp else created.timestamp
            }
        }
        incomingEdge?.also { node._incomingEdge = it }

        event?.also { node._events.add(it) }

        type?.also { node._type = it }
      }
  }

  data class NodeImpl(
    private var nodeMap: Map<Long, NodeImpl>,
    internal var _id: Long,
    internal var _name: String? = null,
    internal var _argument: Any? = null,
    internal var _result: Any? = null,
    internal var _start: Instant? = null,
    internal var _end: Instant? = null,
    internal var _outgoingEdges: MutableMap<Long, InternalEdge> = mutableMapOf(),
    internal var _incomingEdge: InternalEdge? = null,
    internal var _failureReasons: MutableSet<Throwable> = mutableSetOf(),
    internal var _type: OnboardingGraphNode.Type = OnboardingGraphNode.Type.UNKNOWN,
    internal var _component: String? = null,

    // Note that one event can be associated with multiple nodes
    internal var _events: MutableSet<OnboardingGraphLog.OnboardingEvent> = mutableSetOf(),
  ) : OnboardingGraphNode {
    override val id: Long
      get() = _id

    override val name: String
      get() = _name ?: "Unnamed node $id"
    override val argument: Any?
      get() = _argument

    override val result: Any?
      get() = _result

    override val start: Instant
      get() = _start ?: throw IllegalStateException("No start time for node $id")

    override val end: Instant
      get() = _end ?: throw IllegalStateException("No end time for node $id")

    override val outgoingEdges: Set<OutgoingEdge>
      get() =
        _outgoingEdges.values
          .map {
            if (nodeMap.contains(it.id)) {
              OutgoingEdge(checkNotNull(nodeMap[it.id]) { "Node[${it.id} not found" }, it.timestamp)
            } else {
              throw IllegalStateException("$id relies on non-existing outgoing node $it")
            }
          }
          .toSet()

    // It filters the invalid outgoing edges i.e. edges to non existent node and returns only the
    // valid ones.
    override val outgoingEdgesOfValidNodes: Set<OutgoingEdge>
      get() =
        _outgoingEdges.values
          .asSequence()
          .filter { it.id in nodeMap }
          .map {
            OutgoingEdge(checkNotNull(nodeMap[it.id]) { "Node[${it.id} not found" }, it.timestamp)
          }
          .toSet()

    override val incomingEdge: IncomingEdge?
      get() =
        _incomingEdge?.let {
          if (it.id in nodeMap) {
            IncomingEdge(checkNotNull(nodeMap[it.id]) { "Node[${it.id} not found" }, it.timestamp)
          } else {
            throw IllegalStateException("$id relies on non-existing incoming node $it")
          }
        }

    override val failureReasons: Set<Throwable>
      get() = _failureReasons.toSet()

    override val isFailed: Boolean
      get() = failureReasons.isNotEmpty()

    override val type: OnboardingGraphNode.Type
      get() = _type

    override val component: String?
      get() = _component

    override val isSynchronous: Boolean
      get() = type == OnboardingGraphNode.Type.SYNCHRONOUS

    override val events: Set<OnboardingGraphLog.OnboardingEvent>
      get() = _events.toSet()

    override val isComplete: Boolean
      get() = _events.size >= 2

    /**
     * Specifies the duration from the time this node entered the graph until it exited the graph.
     */
    override val totalRuntime: Duration
      get() =
        if (_start == null || _end == null) Duration.ofSeconds(-1) else Duration.between(start, end)

    /**
     * Specifies how long the node was actually active. Defined by [totalRuntime] minus all the time
     * waiting for other node calls initiated by this node to complete.
     */
    override val individualRuntime: Duration
      get() =
        totalRuntime.minus(
          outgoingEdges
            .filter(OutgoingEdge::isOpen)
            .map(OutgoingEdge::node)
            .map(OnboardingGraphNode::totalRuntime)
            .fold(Duration.ZERO) { acc, duration -> acc.plus(duration) }
        )

    override fun hashCode(): Int = Objects.hash(_id, _name)

    override fun equals(other: Any?): Boolean =
      other is NodeImpl && _id == other._id && _name == other._name

    override fun toString(): String =
      "{Node $_id name=$_name, argument=$_argument," +
        " result=$_result, start=$_start, end=$_end," +
        " outgoingEdges=$_outgoingEdges," +
        " incomingEdge=$_incomingEdge}"
  }

  sealed class InternalEdge(open val id: Long, open var timestamp: Instant) {
    data class OutgoingEdge(override val id: Long, override var timestamp: Instant) :
      InternalEdge(id, timestamp)

    data class OpenIncomingEdge(override val id: Long, override var timestamp: Instant) :
      InternalEdge(id, timestamp)

    data class ClosedIncomingEdge(override val id: Long, override var timestamp: Instant) :
      InternalEdge(id, timestamp)
  }

  data class IncomingEdge(override val node: NodeImpl, override val timestamp: Instant) :
    OnboardingGraphEdge.Incoming

  data class OutgoingEdge(override val node: NodeImpl, override val timestamp: Instant) :
    OnboardingGraphEdge.Outgoing {
    val isOpen = node._incomingEdge is InternalEdge.OpenIncomingEdge
  }
}
