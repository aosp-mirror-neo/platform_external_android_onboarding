package com.android.onboarding.nodes

import com.android.onboarding.nodes.OnboardingGraphNodeData.Type
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

internal object OnboardingGraphBuilder {

  fun build(events: Iterable<OnboardingGraphLog.OnboardingEventDelegate>): OnboardingGraph {
    val sortedEvents =
      events
        .distinctBy(OnboardingGraphLog.OnboardingEventDelegate::source)
        .sortedBy(OnboardingGraphLog.OnboardingEventData::timestamp)

    return object : OnboardingGraph {
      override val events: Iterable<OnboardingGraphLog.OnboardingEventDelegate> = sortedEvents
      override val nodes: Map<Long, OnboardingGraphNode> = buildNodes(sortedEvents)
    }
  }

  /** Print a warning message to error stream. */
  private fun warn(message: String) {
    System.err.println("W: $message")
  }

  /**
   * Returns [original] or generated id from this event if [original] is
   * [NodeBuilder.UNKNOWN_NODE_ID].
   */
  private fun OnboardingGraphLog.OnboardingEventDelegate.safeId(original: Long = nodeId) =
    if (original == NodeBuilder.UNKNOWN_NODE_ID) {
      val id = "${source.nodeComponent}/${source.nodeName}".hashCode().toLong().absoluteValue
      warn("Unknown node referenced, generating local id=$id $source")
      id
    } else {
      original
    }

  /** Get an existing [NodeBuilder] or a new stub for [id] and [event]. */
  private inline fun MutableMap<Long, NodeBuilder>.node(
    id: Long,
    event: OnboardingGraphLog.OnboardingEventDelegate,
    onStub: (id: Long) -> Unit = {},
    update: NodeBuilder.Updater.() -> Unit = {},
  ): NodeBuilder {
    val newId = event.safeId(id)
    return getOrPut(newId) {
        onStub(newId)
        NodeBuilder(
          nodeProvider = ::get,
          id = newId,
          unknown = newId != id,
          estimatedStart = event.timestamp,
        )
      }
      .update(event, update)
  }

  /** Get a [NodeBuilder] for [id] and [event] spawned by this node. */
  private inline fun MutableMap<Long, NodeBuilder>.spawnNode(
    id: Long,
    event: OnboardingGraphLog.OnboardingEventDelegate,
    update: NodeBuilder.Updater.() -> Unit = {},
  ) =
    node(id, event) {
      spawnedEvents(event)
      update()
    }

  private fun buildNodes(
    events: Iterable<OnboardingGraphLog.OnboardingEventDelegate>
  ): Map<Long, NodeBuilder> = buildMap {
    /**
     * First pass:
     * - Build nodes from the events they've spawned
     * - Append spawned events
     * - Append outgoing edges
     * - Set obvious types
     * - Prepare second pass
     */
    val secondPass = mutableListOf<OnboardingGraphLog.OnboardingEventDelegate>()
    for (event in events) {
      when (val source = event.source) {
        is OnboardingEvent.ActivityNodeStartExecuteSynchronously -> {
          secondPass += event
          spawnNode(source.sourceNodeId, event) {
            /*
             * Don't create an outgoing edge since [OnboardingEvent.ActivityNodeExecutedForResult]
             * is always spawned after [OnboardingEvent.ActivityNodeExecutedSynchronously]
             */
          }
        }
        is OnboardingEvent.ActivityNodeExecutedForResult -> {
          secondPass += event
          spawnNode(source.sourceNodeId, event) {
            outgoingEdges(
              InternalEdge.OutgoingEdge(
                id = event.safeId(event.nodeId),
                timestamp = event.timestamp,
              )
            )
          }
        }
        is OnboardingEvent.ActivityNodeExecutedDirectly -> {
          secondPass += event
          spawnNode(source.sourceNodeId, event) {
            outgoingEdges(
              InternalEdge.OutgoingEdge(
                id = event.safeId(event.nodeId),
                timestamp = event.timestamp,
              )
            )
          }
        }
        is OnboardingEvent.ActivityNodeExecutedSynchronously,
        is OnboardingEvent.ActivityNodeResultReceived -> {
          /* Spawned at source but does not contain source id */
          secondPass += event
        }
        is OnboardingEvent.ActivityNodeResumedAfterLaunch -> {
          secondPass += event
          spawnNode(event.nodeId, event) {
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
          }
        }
        is OnboardingEvent.ActivityNodeArgumentExtracted -> {
          spawnNode(event.nodeId, event).update(event) {
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
            argument(source.argument)
          }
        }
        is OnboardingEvent.ActivityNodeSetResult -> {
          spawnNode(event.nodeId, event) {
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
            result(source.result)
          }
        }
        is OnboardingEvent.ActivityNodeFail -> {
          spawnNode(event.nodeId, event) {
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
            failureReasons(IllegalStateException(source.reason))
          }
        }
        is OnboardingEvent.ActivityNodeFailedValidation -> {
          spawnNode(event.nodeId, event) {
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
            failureReasons(source.exception)
          }
        }
        is OnboardingEvent.ActivityNodeFinished -> {
          spawnNode(event.nodeId, event) {
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
            finish(source)
          }
        }
        is OnboardingEvent.ActivityNodeResumed,
        is OnboardingEvent.ActivityNodeValidating,
        is OnboardingEvent.ActivityNodeExtractArgument -> {
          spawnNode(event.nodeId, event) {
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
          }
        }
      }
    }

    /**
     * Second pass:
     * - Stub nodes that did not spawn events
     * - Append related events
     * - Append incoming edges
     * - Set remaining types
     * - Prepare third pass
     */
    val thirdPass = mutableListOf<OnboardingGraphLog.OnboardingEventDelegate>()
    for (event in secondPass) {
      when (val source = event.source) {
        is OnboardingEvent.ActivityNodeStartExecuteSynchronously -> {
          node(
            event.nodeId,
            event,
            onStub = { warn("Target node($it) not found in second pass for $event!") },
          ) {
            relatedEvents(event)
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.SYNCHRONOUS)
            argument(source.argument)
            incomingEdge(
              InternalEdge.OpenIncomingEdge(
                id = event.safeId(source.sourceNodeId),
                timestamp = event.timestamp,
              )
            )
          }
        }
        is OnboardingEvent.ActivityNodeExecutedForResult -> {
          node(
            event.nodeId,
            event,
            onStub = { warn("Target node($it) not found in second pass for $event!") },
          ) {
            relatedEvents(event)
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
            argument(source.argument)
            incomingEdge(
              InternalEdge.OpenIncomingEdge(
                id = event.safeId(source.sourceNodeId),
                timestamp = event.timestamp,
              )
            )
          }
        }
        is OnboardingEvent.ActivityNodeExecutedDirectly -> {
          node(
            event.nodeId,
            event,
            onStub = { warn("Target node($it) not found in second pass for $event!") },
          ) {
            relatedEvents(event)
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
            argument(source.argument)
            incomingEdge(
              InternalEdge.ClosedIncomingEdge(
                id = event.safeId(source.sourceNodeId),
                timestamp = event.timestamp,
              )
            )
          }
        }
        is OnboardingEvent.ActivityNodeExecutedSynchronously,
        is OnboardingEvent.ActivityNodeResultReceived -> {
          thirdPass += event
          node(
            event.nodeId,
            event,
            onStub = { warn("Target node($it) not found in second pass for $event!") },
          ) {
            relatedEvents(event)
            name(event.nodeName)
            component(event.nodeComponent)
            type(Type.ACTIVITY)
            if (source is OnboardingEvent.WithResult) result(source.result)
          }
        }
        is OnboardingEvent.ActivityNodeResumedAfterLaunch -> {
          node(
            source.sourceNodeId,
            event,
            onStub = { warn("Target node($it) not found in second pass for $event!") },
          ) {
            relatedEvents(event)
          }
        }
        else -> {
          /* ignored */
        }
      }
    }
    /**
     * Third pass:
     * - Join executions to source nodes via incomingEdges
     */
    for (event in thirdPass) {
      when (event.source) {
        is OnboardingEvent.ActivityNodeExecutedSynchronously -> {
          node(
            event.nodeId,
            event,
            onStub = { warn("Target node($it) not found in third pass for $event!") },
          ) {
            val incomingEdge = node.incomingEdge
            if (incomingEdge == null) {
              warn("Missing incoming edge for ${node.identity} $event")
            } else {
              incomingEdge.node.update(event) { spawnedEvents(event) }
            }
          }
        }
        is OnboardingEvent.ActivityNodeResultReceived -> {
          node(
            event.nodeId,
            event,
            onStub = { warn("Target node($it) not found in third pass for $event!") },
          ) {
            val incomingEdge = node.incomingEdge
            if (incomingEdge == null) {
              warn("Missing incoming edge for ${node.identity} $event")
            } else {
              incomingEdge.node.update(event) { spawnedEvents(event) }
            }
          }
        }
        else -> {
          /* ignored */
        }
      }
    }

    /*
     * Then we need to recursively expand the time of all callers which are waiting for a result -
     * as they haven't finished executing.
     */
    val nodesToRemove = mutableSetOf<Long>()
    for (node in values) {
      var recursiveNode: NodeBuilder? = node
      while (recursiveNode != null) {
        recursiveNode =
          recursiveNode.incomingEdge
            ?.takeIf { it is InternalEdge.OpenIncomingEdge }
            ?.let { incomingEdge ->
              incomingEdge.node.also { incomingNode ->
                /*
                 * If the edge is "Open" then it's waiting for a reply too so the incoming node must
                 * be at least as long as this node
                 */
                incomingNode.update(null) {
                  if (node.start < incomingNode.start) timestamp(node.start)
                  if (node.end > incomingNode.end) timestamp(node.end)
                }
              }
            }
      }

      if (node.isSynchronous && node.events.size == 1) {
        // It only started and did nothing else
        nodesToRemove += node.id
      }
    }
    nodesToRemove.forEach(::remove)
  }

  /** Represents raw graph edges used internally to build proper node edges. */
  private sealed class InternalEdge {
    /** Node ID this edge connects to. */
    abstract val id: Long

    /** The timestamp of the onboarding event that created this edge. */
    abstract val timestamp: Instant

    val node: NodeBuilder
      get() =
        checkNotNull(nodeProvider(id)) {
          "Node[$originId] relies on non-existing outgoing Node[$id]"
        }

    private var nodeProvider: (id: Long) -> NodeBuilder? by Delegates.notNull()
    private var originId: Long by Delegates.notNull()

    /** Inject this edge with lazy metadata. */
    fun inject(nodeProvider: (id: Long) -> NodeBuilder?, originId: Long) {
      this.nodeProvider = nodeProvider
      this.originId = originId
    }

    data class OutgoingEdge(override val id: Long, override var timestamp: Instant) :
      InternalEdge(), OnboardingGraphEdge.Outgoing {
      /**
       * Open outgoing edge implies that the source is finishing and is not expected to resume
       * later.
       */
      val isOpen: Boolean
        get() = node.incomingEdge is OpenIncomingEdge
    }

    sealed class IncomingEdge : InternalEdge(), OnboardingGraphEdge.Incoming

    /** The source node of this edge is waiting for result and is expecting to be resumed. */
    data class OpenIncomingEdge(override val id: Long, override var timestamp: Instant) :
      IncomingEdge()

    /** The source node of this edge is finished and is not expecting to be resumed. */
    data class ClosedIncomingEdge(override val id: Long, override var timestamp: Instant) :
      IncomingEdge()
  }

  private class NodeBuilder(
    private val nodeProvider: (id: Long) -> NodeBuilder?,
    override val id: Long,
    override val unknown: Boolean,
    private val estimatedStart: Instant,
  ) : OnboardingGraphNode {
    override fun toString() = identity

    private val defaultName = IOnboardingGraphNode.unknown(id)
    override var name: String = defaultName
      private set

    private val unknownComponent = IOnboardingGraphNode.unknownComponent(id)
    override var component: String = unknownComponent
      private set

    override var argument: Any? = null
      private set

    override var result: Any? = null
      private set

    override var type = Type.UNKNOWN
      private set

    private var _start: Instant? = null
    override val start: Instant
      get() = _start ?: estimatedStart

    private var tail: Instant? = null
    private var finish: OnboardingEvent? = null

    private var estimatedEnd: Instant? = null
    override val end: Instant
      get() = finish?.timestamp ?: tail ?: estimatedEnd ?: start

    override var outgoingEdges = setOf<InternalEdge.OutgoingEdge>()
      private set

    override val outgoingEdgesOfValidNodes: Collection<InternalEdge.OutgoingEdge>
      get() = outgoingEdges.filter { nodeProvider(it.id) != null }

    override var incomingEdge: InternalEdge.IncomingEdge? = null
      private set

    override var failureReasons = setOf<Throwable>()
      private set

    override var spawnedEvents: Set<OnboardingGraphLog.OnboardingEventDelegate> = emptySet()
      private set

    override var relatedEvents: Set<OnboardingGraphLog.OnboardingEventDelegate> = emptySet()
      private set

    @CanIgnoreReturnValue
    inline fun update(
      event: OnboardingGraphLog.OnboardingEventDelegate?,
      block: Updater.() -> Unit,
    ): NodeBuilder {
      Updater(event).apply(block)
      return this
    }

    override var issues: Collection<String> = listOf()
      private set

    /** Print a warning message to error stream and add it to [issues]. */
    private fun warn(message: String) {
      System.err.println("W: $message")
      issues += message
    }

    inner class Updater(delegate: OnboardingGraphLog.OnboardingEventDelegate?) {
      private val event = delegate?.source
      val node = this@NodeBuilder
      private val eventRef = event?.toString() ?: ""
      private val nodeProvider: (Long) -> NodeBuilder? = {
        this@NodeBuilder.nodeProvider(event?.safeId(it) ?: it)
      }

      fun name(value: String?) {
        if (value != null && value != defaultName && value != name) {
          validate(
            condition = name == defaultName,
            onFailure = { "Previously set name '$name' cannot be overridden with '$value'" },
          ) {
            name = value
          }
        }
      }

      fun component(value: String?) {
        if (value != null && value != unknownComponent && value != component) {
          validate(
            condition = component == unknownComponent,
            onFailure = {
              "Previously set component '$component' cannot be overridden with '$value'"
            },
          ) {
            component = value
          }
        }
      }

      fun argument(value: Any?) {
        validate(
          condition = argument == null || value == argument,
          onFailure = { "Previously set argument '$argument' cannot be overridden with '$value'" },
        ) {
          argument = value
        }
      }

      fun result(value: Any?) {
        validate(
          condition = result == null || value == result,
          onFailure = { "Previously set result '$result' cannot be overridden with '$value'" },
        ) {
          result = value
        }
      }

      fun type(value: Type?) {
        if (value != null && (type == Type.UNKNOWN || value == Type.SYNCHRONOUS)) {
          type = value
        }
      }

      fun timestamp(value: Instant) {
        val s = _start
        if (s == null || value < s) _start = value
        val t = tail
        if (t != null && value > t) tail = value
      }

      fun finish(value: OnboardingEvent.ActivityNodeFinished) {
        validate(
          condition = finish == null,
          onFailure = {
            "Duplicate finish events detected for $identity! prev=$finish, next=$value"
          },
        ) {
          finish =
            maxOf(
              a = value,
              b = finish ?: value,
              comparator = OnboardingGraphLog.OnboardingEventData::compareTo,
            )
        }
      }

      fun outgoingEdges(vararg values: InternalEdge.OutgoingEdge?) {
        outgoingEdges +=
          values.filterNotNull().onEach { it.inject(nodeProvider = nodeProvider, originId = id) }
      }

      fun incomingEdge(value: InternalEdge.IncomingEdge?) {
        incomingEdge =
          value?.also { it.inject(nodeProvider = nodeProvider, originId = id) } ?: incomingEdge
      }

      fun failureReasons(vararg values: Throwable?) {
        failureReasons += values.filterNotNull()
      }

      fun spawnedEvents(vararg values: OnboardingGraphLog.OnboardingEventDelegate?) {
        spawnedEvents +=
          values.filterNotNull().onEach {
            val s = _start
            if (s == null || it.timestamp < s) _start = it.timestamp
            val t = tail
            if (t == null || it.timestamp > t) tail = it.timestamp
          }
      }

      fun relatedEvents(vararg values: OnboardingGraphLog.OnboardingEventDelegate?) {
        relatedEvents +=
          values.filterNotNull().onEach {
            val ee = estimatedEnd
            if (ee == null || ee < it.timestamp) estimatedEnd = it.timestamp
          }
      }

      private inline fun validate(
        condition: Boolean,
        onFailure: () -> String,
        onSuccess: () -> Unit,
      ) {
        if (!condition) {
          val failure = onFailure()
          warn("$failure $identity $eventRef")
        } else {
          onSuccess()
        }
      }
    }
    override val isComplete: Boolean
      get() = events.size >= 2

    override val pausedRuntime: Duration
      get() {
        var total = Duration.ZERO
        var pause: OnboardingEvent? = null
        val closures = mutableListOf<OnboardingEvent>()
        for (event in spawnedEvents.sortedBy(OnboardingGraphLog.OnboardingEventData::timestamp)) {
          when (val source = event.source) {
            is OnboardingEvent.ActivityNodeExecutedDirectly -> {
              if (pause != null) {
                warn(
                  "Closure event without terminating previous suspend event for $identity: prev=$pause, next=$source"
                )
              } else {
                pause = null
                closures += source
              }
            }

            /**
             * We don't need to check for [OnboardingEvent.ActivityNodeStartExecuteSynchronously]
             * since it is always spawned in tandem with
             * [OnboardingEvent.ActivityNodeExecutedForResult].
             */
            is OnboardingEvent.ActivityNodeExecutedForResult -> {
              if (closures.isNotEmpty()) {
                warn("New suspend event after closure for $identity: $source")
              } else if (pause != null) {
                warn(
                  "New suspend event without terminating previous suspend event for $identity: prev=$pause, next=$source"
                )
              } else {
                pause = source
              }
            }
            is OnboardingEvent.ActivityNodeExecutedSynchronously,
            is OnboardingEvent.ActivityNodeResultReceived -> {
              if (closures.isNotEmpty()) {
                warn("Resume event after closure for $identity: $source")
              } else if (pause == null) {
                warn("Resume event without previous suspend event for $identity: $source")
              } else {
                if (event.timestamp > end) {
                  warn("Resume event after end time for $identity: end=$end, event=$source")
                } else {
                  total += Duration.between(pause.timestamp, event.timestamp)
                }
                pause = null
              }
            }
            is OnboardingEvent.ActivityNodeStartExecuteSynchronously,
            is OnboardingEvent.ActivityNodeFinished,
            is OnboardingEvent.ActivityNodeResumedAfterLaunch,
            is OnboardingEvent.ActivityNodeResumed,
            is OnboardingEvent.ActivityNodeFailedValidation,
            is OnboardingEvent.ActivityNodeFail,
            is OnboardingEvent.ActivityNodeExtractArgument,
            is OnboardingEvent.ActivityNodeSetResult,
            is OnboardingEvent.ActivityNodeValidating,
            is OnboardingEvent.ActivityNodeArgumentExtracted -> {
              /* ignore */
            }
          }
        }
        if (closures.size > 1) {
          warn("Multiple closure events for $identity: $closures")
        }
        return total
      }

    companion object {
      const val UNKNOWN_NODE_ID = -1L
    }
  }
}
