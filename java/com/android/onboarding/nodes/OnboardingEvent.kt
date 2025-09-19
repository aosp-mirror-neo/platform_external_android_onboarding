package com.android.onboarding.nodes

import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.metadata.EncodedMetadata
import com.android.onboarding.tasks.utils.TaskResult
import com.google.common.io.BaseEncoding
import java.time.Instant

/** Possible Events. */
@OptIn(InternalOnboardingApi::class)
sealed interface OnboardingEvent : OnboardingGraphLog.OnboardingEventDelegate {

  // LINT.IfChange
  override val source: OnboardingEvent
    get() = this

  /** Writes this event to a string which can be parsed by [OnboardingEvent.deserialize]. */
  fun serializeToString(): String {
    return BaseEncoding.base64().encode(serialize().toByteArray())
  }

  fun serialize(): OnboardingProtos.LogProto

  /** Marker interface for events containing an intent data. */
  interface WithIntent {
    /** An intent related to this event. */
    val intent: IntentData?
  }

  /** Marker interface for events containing a result. */
  interface WithResult {
    /** A result produced by the target node. */
    val result: Any?
  }

  /** Interface for events containing metadata. */
  interface WithMetadata {
    /** The [List] of metadata encoded */
    val metadata: List<EncodedMetadata>
  }

  /** Marker interface for events containing an argument. */
  interface WithArgument {
    /** The argument supplied for launching target node with. */
    val argument: Any?
  }

  /** Marker interface for events containing a source node id. */
  interface WithSource {
    /** The id of the source node being interacted with. */
    val sourceNodeId: Long
  }

  /** An [OnboardingEvent] spawned only from contracts implemented by activities. */
  sealed class OnboardingActivityEvent : OnboardingEvent {
    protected abstract val nodeRef: NodeRef.Stable
    final override val nodeComponent: String
      get() = nodeRef.nodeComponent

    final override val nodeName: String
      get() = nodeRef.nodeName
  }

  /**
   * At [timestamp], a node with id [nodeId] has prepared a PendingContract for target
   * [nodeComponent]/[nodeName] launch with supplied [argument] without actually launching it.
   *
   * Spawned at [nodeId].
   *
   * @property nodeId the id of the node that prepared the PendingContract.
   * @property nodeRef the reference to the node being prepared.
   */
  data class PendingContractPreparedForActivity(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: NodeId,
    override val argument: Any?,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithArgument {
    constructor(
      nodeRef: NodeRef,
      nodeId: NodeId,
      argument: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, argument, timestamp)

    constructor(
      proto: OnboardingProtos.PendingContractPreparedForActivityProto,
      timestamp: Instant?,
    ) : this(
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      nodeId = proto.nodeId,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
      argument = null,
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setPendingContractPreparedForActivity(
          OnboardingProtos.PendingContractPreparedForActivityProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // .setArgument(argument)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], a node with id [nodeId] has prepared a PendingContract for target
   * [nodeComponent]/[nodeName] launch with supplied [argument] without actually launching it.
   *
   * Spawned at [nodeId].
   *
   * @property nodeId the id of the node that prepared the PendingContract.
   * @property nodeName target node name.
   * @property nodeComponent target node component.
   */
  data class PendingContractPreparedForTask(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: NodeId,
    override val argument: Any?,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithArgument {
    constructor(
      nodeRef: NodeRef,
      nodeId: NodeId,
      argument: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, argument, timestamp)

    constructor(
      proto: OnboardingProtos.PendingContractPreparedForTaskProto,
      timestamp: Instant?,
    ) : this(
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      nodeId = proto.nodeId,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
      argument = null,
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setPendingContractPreparedForTask(
          OnboardingProtos.PendingContractPreparedForTaskProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // .setArgument(argument)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], a node with id [sourceNodeId] has prepared a PendingContract for target
   * [nodeComponent]/[nodeName] launch with supplied [argument] without actually launching it. The
   * launch was assigned the id of [launchId] which can be looked up by in other events to follow
   * the lifecycle of this PendingContract.
   *
   * Spawned at [sourceNodeId].
   *
   * @property nodeId target node id.
   * @property nodeName target node name.
   * @property nodeComponent target node component.
   * @property launchId unique id assigned to track the lifecycle of the PendingContract.
   * @property creatorNodeId the id of the node that prepared the PendingContract.
   * @property sourceNodeId the id of the node launching the PendingContract.
   */
  data class PendingContractLaunchedForActivity(
    override val nodeId: Long,
    override val nodeRef: NodeRef.Stable,
    override val sourceNodeId: Long,
    val creatorNodeId: Long,
    val launchId: Long,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithSource {
    constructor(
      nodeId: Long,
      nodeRef: NodeRef,
      sourceNodeId: Long,
      creatorNodeId: Long,
      launchId: Long,
      timestamp: Instant = Instant.now(),
    ) : this(nodeId, nodeRef.asNodeRef(), sourceNodeId, creatorNodeId, launchId, timestamp)

    constructor(
      proto: OnboardingProtos.PendingContractLaunchedForActivityProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      sourceNodeId = proto.sourceNodeId,
      creatorNodeId = proto.creatorNodeId,
      launchId = proto.launchId,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setPendingContractLaunchedForActivity(
          OnboardingProtos.PendingContractLaunchedForActivityProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            .setSourceNodeId(sourceNodeId)
            .setCreatorNodeId(creatorNodeId)
            .setLaunchId(launchId)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], a target [android.app.Activity] with id [nodeId] has been launched from
   * [sourceNodeId] by invoking the [nodeName] contract from inside the [sourceNodeId] node with
   * [argument] without expecting a result. This implies that [sourceNodeId] is finishing and does
   * not expect to be returned to.
   *
   * Spawned at [sourceNodeId].
   *
   * @property nodeId target node id.
   * @property nodeName target node name.
   * @property nodeComponent target node component.
   * @property sourceNodeId source node id.
   */
  data class ActivityNodeExecutedDirectly(
    override val nodeRef: NodeRef.Stable,
    override val sourceNodeId: Long,
    override val nodeId: Long,
    override val argument: Any?,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithArgument, WithSource {
    constructor(
      nodeRef: NodeRef,
      sourceNodeId: Long,
      nodeId: Long,
      argument: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), sourceNodeId, nodeId, argument, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeExecutedDirectlyProto,
      timestamp: Instant?,
    ) : this(
      sourceNodeId = proto.sourceNodeId,
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
      argument = null,
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeExecutedDirectly(
          OnboardingProtos.ActivityNodeExecutedDirectlyProto.newBuilder()
            .setSourceNodeId(sourceNodeId)
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // .setArgument(argument)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] is using the [nodeName] contract to
   * validate intent [intent].
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeValidating(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: Long,
    override val intent: IntentData?,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithIntent {
    constructor(
      nodeRef: NodeRef,
      nodeId: Long,
      intent: IntentData? = null,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, intent, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeValidatingProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeValidating(
          OnboardingProtos.ActivityNodeValidatingProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // .setIntent(intent)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] has failed validation of [intent]
   * using the [nodeName] contract. The exception was [exception].
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeFailedValidation(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: Long,
    val exception: Throwable,
    override val intent: IntentData?,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithIntent {
    constructor(
      nodeRef: NodeRef,
      nodeId: Long,
      exception: Throwable = IllegalArgumentException("Failed validation"),
      intent: IntentData? = null,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, exception, intent, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeFailedValidationProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeFailedValidation(
          OnboardingProtos.ActivityNodeFailedValidationProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // .setException(exception)
            // .setIntent(IntentData(intent?.action ?: "", mapOf()))

            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] is extracting the argument from
   * [intent] using the [nodeName] contract.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeExtractArgument(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: Long,
    override val intent: IntentData,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithIntent {
    constructor(
      nodeRef: NodeRef,
      nodeId: Long,
      intent: IntentData,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, intent, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeExtractArgumentProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      intent =
        IntentData(
          if (proto.hasIntent()) proto.intent.action else "",
          mapOf(),
        ),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeExtractArgument(
          OnboardingProtos.ActivityNodeExtractArgumentProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            .setIntent(
              OnboardingProtos.IntentDataProto.newBuilder().setAction(intent.action ?: "").build()
            )
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] has extracted [argument] using the
   * [nodeName] contract.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeArgumentExtracted(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: Long,
    override val argument: Any?,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithArgument {
    constructor(
      nodeRef: NodeRef,
      nodeId: Long,
      argument: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, argument, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeArgumentExtractedProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      argument = null,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeArgumentExtracted(
          OnboardingProtos.ActivityNodeArgumentExtractedProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // setArgument(argument)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] has set the result to [result] using
   * the [nodeName] contract.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeSetResult(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: Long,
    override val result: Any?,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithResult {
    constructor(
      nodeRef: NodeRef,
      nodeId: Long,
      result: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, result, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeSetResultProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      result = null,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeSetResult(
          OnboardingProtos.ActivityNodeSetResultProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // setResult(result)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] has failed the [nodeName] contract
   * because of [reason].
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeFail(
    override val nodeId: Long,
    val reason: String?,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent() {
    constructor(
      proto: OnboardingProtos.ActivityNodeFailProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      reason = proto.reason,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    override val nodeRef = NodeRef(ComponentRef(unknownComponent(nodeId)), unknown(nodeId))

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeFail(
          OnboardingProtos.ActivityNodeFailProto.newBuilder()
            .setNodeId(nodeId)
            .setReason(reason ?: "")
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], a target [android.app.Activity] with id [nodeId] has been launched by invoking
   * the [nodeName] contract from inside the [sourceNodeId] node with [argument] while expecting a
   * result. Concluded by matching [ActivityNodeResultReceived] event.
   *
   * Spawned at [sourceNodeId].
   *
   * @property nodeId target node id.
   * @property nodeName target node name.
   * @property nodeComponent target node component.
   * @property sourceNodeId source node id.
   * @property argument the argument supplied for launching target node with.
   */
  data class ActivityNodeExecutedForResult(
    override val nodeRef: NodeRef.Stable,
    override val sourceNodeId: Long,
    override val nodeId: Long,
    override val argument: Any?,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithSource, WithArgument {
    constructor(
      nodeRef: NodeRef,
      sourceNodeId: Long,
      nodeId: Long,
      argument: Any? = null,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), sourceNodeId, nodeId, argument, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeExecutedForResultProto,
      timestamp: Instant?,
    ) : this(
      sourceNodeId = proto.sourceNodeId,
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeExecutedForResult(
          OnboardingProtos.ActivityNodeExecutedForResultProto.newBuilder()
            .setSourceNodeId(sourceNodeId)
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // .setArgument(argument)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], the [result] of the [android.app.Activity] node with id [nodeId] was received.
   *
   * Note that this does not specify which node received the result. The event must be matched up
   * with a previous [ActivityNodeExecutedForResult] event.
   *
   * Spawned at source node.
   *
   * @property nodeName name of the target node.
   * @property nodeComponent name of the target node.
   * @property nodeId id of the target node.
   * @property result a result produced by the target node.
   */
  data class ActivityNodeResultReceived(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: Long,
    override val result: Any?,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithResult {
    constructor(
      nodeRef: NodeRef,
      nodeId: Long,
      result: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, result, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeResultReceivedProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      result = null,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeResultReceived(
          OnboardingProtos.ActivityNodeResultReceivedProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // setResult(result)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], the activity node with id [nodeId] called finish.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeFinished(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: Long,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent() {
    constructor(
      nodeRef: NodeRef,
      nodeId: Long,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeFinishedProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeFinished(
          OnboardingProtos.ActivityNodeFinishedProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], the activity node with id [nodeId] resumes after it previously launched node
   * with id [sourceNodeId].
   *
   * Spawned at [nodeId].
   *
   * @property sourceNodeId the id of the node that this node previously launched and now resumed
   *   from.
   * @property nodeId the id of the node spawning this event.
   * @property nodeName the name of the node spawning this event.
   * @property nodeComponent the component of the node spawning this event.
   */
  data class ActivityNodeResumedAfterLaunch(
    override val nodeRef: NodeRef.Stable,
    override val sourceNodeId: Long,
    override val nodeId: Long,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent(), WithSource {
    constructor(
      nodeRef: NodeRef,
      sourceNodeId: Long,
      nodeId: Long,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), sourceNodeId, nodeId, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeResumedAfterLaunchProto,
      timestamp: Instant?,
    ) : this(
      sourceNodeId = proto.sourceNodeId,
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeResumedAfterLaunch(
          OnboardingProtos.ActivityNodeResumedAfterLaunchProto.newBuilder()
            .setSourceNodeId(sourceNodeId)
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], the activity node with id [nodeId] called finish.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeResumed(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: Long,
    override val timestamp: Instant,
  ) : OnboardingActivityEvent() {
    constructor(
      nodeRef: NodeRef,
      nodeId: Long,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeResumedProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeResumed(
          OnboardingProtos.ActivityNodeResumedProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /** An [OnboardingActivityEvent] spawned only from synchronous executions of contracts. */
  sealed class OnboardingSynchronousActivityEvent : OnboardingActivityEvent()

  /**
   * At [timestamp], the node with id [sourceNodeId] began synchronous execution of the node with id
   * [nodeId]. Concluded by matching [ActivityNodeExecutedSynchronously] event. Note that
   * [OnboardingEvent.ActivityNodeExecutedForResult] will always be spawned immediately after this
   * event in the same flow.
   *
   * Spawned at [sourceNodeId].
   *
   * @property nodeId target node id.
   * @property nodeName target node name.
   * @property nodeComponent target node component.
   * @property sourceNodeId source node id.
   * @property argument the argument supplied for launching target node with.
   */
  data class ActivityNodeStartExecuteSynchronously(
    override val nodeRef: NodeRef.Stable,
    override val sourceNodeId: Long,
    override val nodeId: Long,
    override val argument: Any?,
    override val timestamp: Instant,
  ) : OnboardingSynchronousActivityEvent(), WithSource, WithArgument {
    constructor(
      nodeRef: NodeRef,
      sourceNodeId: Long,
      nodeId: Long,
      argument: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), sourceNodeId, nodeId, argument, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeStartExecuteSynchronouslyProto,
      timestamp: Instant?,
    ) : this(
      sourceNodeId = proto.sourceNodeId,
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      argument = null,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeStartExecuteSynchronously(
          OnboardingProtos.ActivityNodeStartExecuteSynchronouslyProto.newBuilder()
            .setSourceNodeId(sourceNodeId)
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // setArgument(argument)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], the synchronously executing node with id [nodeId] produced a [result].
   *
   * Note that this does not specify which node received the result. The event must be matched up
   * with a previous [ActivityNodeStartExecuteSynchronously] event.
   *
   * Spawned at source node.
   *
   * @property nodeName name of the target node.
   * @property nodeComponent name of the target node.
   * @property nodeId id of the target node.
   */
  data class ActivityNodeExecutedSynchronously(
    override val nodeRef: NodeRef.Stable,
    override val nodeId: Long,
    override val result: Any?,
    override val timestamp: Instant,
  ) : OnboardingSynchronousActivityEvent(), WithResult {
    constructor(
      nodeRef: NodeRef,
      nodeId: Long,
      result: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(nodeRef.asNodeRef(), nodeId, result, timestamp)

    constructor(
      proto: OnboardingProtos.ActivityNodeExecutedSynchronouslyProto,
      timestamp: Instant?,
    ) : this(
      nodeId = proto.nodeId,
      nodeRef = NodeRef(ComponentRef(proto.nodeComponent), proto.nodeName),
      result = null,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setActivityNodeExecutedSynchronously(
          OnboardingProtos.ActivityNodeExecutedSynchronouslyProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // setResult(result)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  // LINT.ThenChange(//depot/google3/logs/proto/android_onboarding/node_interaction_metadata.proto)

  /**
   * Wrapper around the same information as [android.content.Intent] but can be used without
   * dependency on Android.
   */
  data class IntentData(val action: String?, val extras: Map<String, Any?>)

  /** Marker interface for task events. */
  sealed interface OnboardingTaskEvent : OnboardingEvent

  /**
   * At [timestamp], the task started executing with id [nodeId] and [argument].
   *
   * @property nodeName name of the target node.
   * @property nodeComponent name of the target node.
   * @property nodeId id of the target node.
   */
  data class OnboardingTaskStartedEvent(
    override val sourceNodeId: Long = -1,
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val argument: Any?,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingTaskEvent, WithArgument, WithSource {

    constructor(
      proto: OnboardingProtos.OnboardingTaskStartProto,
      timestamp: Instant?,
    ) : this(
      nodeName = proto.nodeName,
      nodeComponent = proto.nodeComponent,
      nodeId = proto.nodeId,
      argument = null,
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setOnboardingTaskStartProto(
          OnboardingProtos.OnboardingTaskStartProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            // setArgument(argument)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], the task with id [nodeId] had a status update [metadata].
   *
   * @property nodeName name of the target node.
   * @property nodeComponent name of the target node.
   * @property nodeId id of the target node.
   */
  data class OnboardingTaskStatusUpdateEvent(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val metadata: List<EncodedMetadata> = listOf(),
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingTaskEvent, WithMetadata {

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setOnboardingTaskStatusUpdateProto(
          OnboardingProtos.OnboardingTaskStatusUpdateProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  /**
   * At [timestamp], the task with id [nodeId] completed execution with [result] and
   * [encodedMetadatas].
   *
   * @property nodeName name of the target node.
   * @property nodeComponent name of the target node.
   * @property nodeId id of the target node.
   */
  data class OnboardingTaskCompleteEvent(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val result: TaskResult<*>,
    override val metadata: List<EncodedMetadata> = listOf(),
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingTaskEvent, WithMetadata, WithResult {

    constructor(
      proto: OnboardingProtos.OnboardingTaskCompleteProto,
      timestamp: Instant?,
    ) : this(
      nodeName = proto.nodeName,
      nodeComponent = proto.nodeComponent,
      nodeId = proto.nodeId,
      result = if (proto.result) TaskResult.Success(Unit) else TaskResult.Failure(),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setOnboardingTaskCompleteProto(
          OnboardingProtos.OnboardingTaskCompleteProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            .setResult(result is TaskResult.Success)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  data class OnboardingTaskCacheWriteEvent(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val result: Any?,
    override val metadata: List<EncodedMetadata> = listOf(),
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingTaskEvent, WithResult, WithMetadata {
    constructor(
      proto: OnboardingProtos.OnboardingTaskCacheWriteProto,
      timestamp: Instant?,
    ) : this(
      nodeName = proto.nodeName,
      nodeComponent = proto.nodeComponent,
      nodeId = proto.nodeId,
      result = null,
      metadata = listOf(),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setOnboardingTaskCacheWriteProto(
          OnboardingProtos.OnboardingTaskCacheWriteProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  data class OnboardingTaskCacheReadEvent(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    val hit: Boolean,
    override val result: Any?,
    override val metadata: List<EncodedMetadata> = listOf(),
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingTaskEvent, WithResult, WithMetadata {
    constructor(
      proto: OnboardingProtos.OnboardingTaskCacheReadProto,
      timestamp: Instant?,
    ) : this(
      nodeName = proto.nodeName,
      nodeComponent = proto.nodeComponent,
      nodeId = proto.nodeId,
      hit = proto.hit,
      result = null,
      metadata = listOf(),
      timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
    )

    init {
      OnboardingNode.validateNodeName(nodeName)
    }

    override fun serialize(): OnboardingProtos.LogProto {
      return OnboardingProtos.LogProto.newBuilder()
        .setOnboardingTaskCacheReadProto(
          OnboardingProtos.OnboardingTaskCacheReadProto.newBuilder()
            .setNodeId(nodeId)
            .setNodeName(nodeName)
            .setNodeComponent(nodeComponent)
            .setHit(hit)
            .setTimestamp(timestamp.toEpochMilli())
            .build()
        )
        .build()
    }
  }

  companion object {
    /**
     * Argument value representing arguments which had their type data erased due to preemptive
     * transformation to transfer layer types.
     */
    const val ERASED_ARGUMENT = "<ERASED>"

    /** Generate standard name for node with [id] and no name. */
    fun unknown(id: Long) = "Unknown(id=$id)"

    /** Generate standard name for node with [id] and no [component]. */
    fun unknownComponent(id: Long) = "UnknownComponent(nodeId=$id)"

    fun deserialize(str: String, timestamp: Instant? = null): OnboardingEvent {
      val proto = OnboardingProtos.LogProto.parseFrom(BaseEncoding.base64().decode(str))


      return when {
        proto.hasPendingContractPreparedForActivity() ->
          PendingContractPreparedForActivity(proto.pendingContractPreparedForActivity, timestamp)

        proto.hasPendingContractLaunchedForActivity() ->
          PendingContractLaunchedForActivity(proto.pendingContractLaunchedForActivity, timestamp)

        proto.hasPendingContractPreparedForTask() ->
          PendingContractPreparedForTask(proto.pendingContractPreparedForTask, timestamp)

        proto.hasActivityNodeExecutedDirectly() ->
          ActivityNodeExecutedDirectly(proto.activityNodeExecutedDirectly, timestamp)

        proto.hasActivityNodeExecutedForResult() ->
          ActivityNodeExecutedForResult(proto.activityNodeExecutedForResult, timestamp)

        proto.hasActivityNodeValidating() ->
          ActivityNodeValidating(proto.activityNodeValidating, timestamp)

        proto.hasActivityNodeFailedValidation() ->
          ActivityNodeFailedValidation(proto.activityNodeFailedValidation, timestamp)

        proto.hasActivityNodeExtractArgument() ->
          ActivityNodeExtractArgument(proto.activityNodeExtractArgument, timestamp)

        proto.hasActivityNodeArgumentExtracted() ->
          ActivityNodeArgumentExtracted(proto.activityNodeArgumentExtracted, timestamp)

        proto.hasActivityNodeSetResult() ->
          ActivityNodeSetResult(proto.activityNodeSetResult, timestamp)

        proto.hasActivityNodeFail() -> ActivityNodeFail(proto.activityNodeFail, timestamp)
        proto.hasActivityNodeResultReceived() ->
          ActivityNodeResultReceived(proto.activityNodeResultReceived, timestamp)

        proto.hasActivityNodeStartExecuteSynchronously() ->
          ActivityNodeStartExecuteSynchronously(
            proto.activityNodeStartExecuteSynchronously,
            timestamp,
          )

        proto.hasActivityNodeExecutedSynchronously() ->
          ActivityNodeExecutedSynchronously(proto.activityNodeExecutedSynchronously, timestamp)

        proto.hasActivityNodeFinished() ->
          ActivityNodeFinished(proto.activityNodeFinished, timestamp)

        proto.hasActivityNodeResumedAfterLaunch() ->
          ActivityNodeResumedAfterLaunch(proto.activityNodeResumedAfterLaunch, timestamp)

        proto.hasActivityNodeResumed() -> ActivityNodeResumed(proto.activityNodeResumed, timestamp)

        proto.hasOnboardingTaskStartProto() ->
          OnboardingTaskStartedEvent(proto.onboardingTaskStartProto, timestamp)
        proto.hasOnboardingTaskCompleteProto() ->
          OnboardingTaskCompleteEvent(proto.onboardingTaskCompleteProto, timestamp)

        proto.hasOnboardingTaskCacheReadProto() ->
          OnboardingTaskCacheReadEvent(proto.onboardingTaskCacheReadProto, timestamp)
        proto.hasOnboardingTaskCacheWriteProto() ->
          OnboardingTaskCacheWriteEvent(proto.onboardingTaskCacheWriteProto, timestamp)

        else -> throw IllegalStateException("Could not parse $proto")
      }
    }
  }
}
