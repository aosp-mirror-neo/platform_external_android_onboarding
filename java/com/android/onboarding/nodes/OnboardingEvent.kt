package com.android.onboarding.nodes

import com.android.onboarding.OnboardingProtos
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.google.common.io.BaseEncoding
import java.time.Instant

/** Possible Events. */
sealed interface OnboardingEvent : OnboardingGraphLog.OnboardingEventDelegate {
  override val nodeName: String
  override val nodeComponent: String
  override val nodeId: Long
  override val timestamp: Instant

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
  sealed interface OnboardingActivityEvent : OnboardingEvent

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
    override val nodeName: String,
    override val nodeComponent: String,
    override val sourceNodeId: Long,
    override val nodeId: Long,
    override val argument: Any?,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent, WithArgument, WithSource {
    constructor(
      sourceNodeId: Long,
      nodeId: Long,
      nodeClass: Class<*>,
      argument: Any? = null,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      sourceNodeId = sourceNodeId,
      nodeId = nodeId,
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      argument = argument,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(
        proto: OnboardingProtos.ActivityNodeExecutedDirectlyProto,
        timestamp: Instant?,
      ) =
        ActivityNodeExecutedDirectly(
          sourceNodeId = proto.sourceNodeId,
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
          argument = null,
        )
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] is using the [nodeName] contract to
   * validate intent [intent].
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeValidating(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val intent: IntentData? = null,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent, WithIntent {
    constructor(
      nodeId: Long,
      nodeClass: Class<*>,
      intent: IntentData? = null,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      intent = intent,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(proto: OnboardingProtos.ActivityNodeValidatingProto, timestamp: Instant?) =
        ActivityNodeValidating(
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] has failed validation of [intent]
   * using the [nodeName] contract. The exception was [exception].
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeFailedValidation(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    val exception: Throwable = IllegalArgumentException("Failed validation"),
    override val intent: IntentData? = null,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent, WithIntent {
    constructor(
      nodeId: Long,
      nodeClass: Class<*>,
      exception: Throwable = IllegalArgumentException("Failed validation"),
      intent: IntentData? = null,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      exception = exception,
      intent = intent,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(
        proto: OnboardingProtos.ActivityNodeFailedValidationProto,
        timestamp: Instant?,
      ) =
        ActivityNodeFailedValidation(
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] is extracting the argument from
   * [intent] using the [nodeName] contract.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeExtractArgument(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val intent: IntentData,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent, WithIntent {
    constructor(
      nodeId: Long,
      nodeClass: Class<*>,
      intent: IntentData,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      intent = intent,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(proto: OnboardingProtos.ActivityNodeExtractArgumentProto, timestamp: Instant?) =
        ActivityNodeExtractArgument(
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          intent =
            IntentData(
              if (proto.hasIntent()) proto.intent.action else "",
              mapOf(),
            ),
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] has extracted [argument] using the
   * [nodeName] contract.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeArgumentExtracted(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val argument: Any?,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent, WithArgument {
    constructor(
      nodeId: Long,
      nodeClass: Class<*>,
      argument: Any? = null,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      argument = argument,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(
        proto: OnboardingProtos.ActivityNodeArgumentExtractedProto,
        timestamp: Instant?,
      ) =
        ActivityNodeArgumentExtracted(
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          argument = null,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
    }
  }

  /**
   * At [timestamp], an [android.app.Activity] with id [nodeId] has set the result to [result] using
   * the [nodeName] contract.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeSetResult(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val result: Any?,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent, WithResult {
    constructor(
      nodeId: Long,
      nodeClass: Class<*>,
      result: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      result = result,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(proto: OnboardingProtos.ActivityNodeSetResultProto, timestamp: Instant?) =
        ActivityNodeSetResult(
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          result = null,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
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
  ) : OnboardingActivityEvent {
    override val nodeName: String = IOnboardingGraphNode.unknown(nodeId)
    override val nodeComponent: String = IOnboardingGraphNode.unknownComponent(nodeId)

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

    companion object {
      fun fromProto(proto: OnboardingProtos.ActivityNodeFailProto, timestamp: Instant?) =
        ActivityNodeFail(
          nodeId = proto.nodeId,
          reason = proto.reason,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
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
    override val nodeName: String,
    override val nodeComponent: String,
    override val sourceNodeId: Long,
    override val nodeId: Long,
    override val argument: Any? = null,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent, WithSource, WithArgument {
    constructor(
      sourceNodeId: Long,
      nodeId: Long,
      nodeClass: Class<*>,
      argument: Any? = null,
      timestamp: Instant = Instant.now(),
    ) : this(
      sourceNodeId = sourceNodeId,
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      argument = argument,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(
        proto: OnboardingProtos.ActivityNodeExecutedForResultProto,
        timestamp: Instant?,
      ) =
        ActivityNodeExecutedForResult(
          sourceNodeId = proto.sourceNodeId,
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
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
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val result: Any?,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent, WithResult {
    constructor(
      nodeId: Long,
      nodeClass: Class<*>,
      result: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      result = result,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(proto: OnboardingProtos.ActivityNodeResultReceivedProto, timestamp: Instant?) =
        ActivityNodeResultReceived(
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          result = null,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
    }
  }

  /**
   * At [timestamp], the activity node with id [nodeId] called finish.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeFinished(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent {
    constructor(
      nodeId: Long,
      nodeClass: Class<*>,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(proto: OnboardingProtos.ActivityNodeFinishedProto, timestamp: Instant?) =
        ActivityNodeFinished(
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
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
    override val nodeName: String,
    override val nodeComponent: String,
    override val sourceNodeId: Long,
    override val nodeId: Long,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent, WithSource {
    constructor(
      sourceNodeId: Long,
      nodeId: Long,
      nodeClass: Class<*>,
      timestamp: Instant = Instant.now(),
    ) : this(
      sourceNodeId = sourceNodeId,
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(
        proto: OnboardingProtos.ActivityNodeResumedAfterLaunchProto,
        timestamp: Instant?,
      ) =
        ActivityNodeResumedAfterLaunch(
          sourceNodeId = proto.sourceNodeId,
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
    }
  }

  /**
   * At [timestamp], the activity node with id [nodeId] called finish.
   *
   * Spawned at [nodeId].
   */
  data class ActivityNodeResumed(
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingActivityEvent {
    constructor(
      nodeId: Long,
      nodeClass: Class<*>,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(proto: OnboardingProtos.ActivityNodeResumedProto, timestamp: Instant?) =
        ActivityNodeResumed(
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
    }
  }

  /** An [OnboardingActivityEvent] spawned only from synchronous executions of contracts. */
  sealed interface OnboardingSynchronousActivityEvent : OnboardingActivityEvent

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
    override val nodeName: String,
    override val nodeComponent: String,
    override val sourceNodeId: Long,
    override val nodeId: Long,
    override val argument: Any?,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingSynchronousActivityEvent, WithSource, WithArgument {
    constructor(
      sourceNodeId: Long,
      nodeId: Long,
      nodeClass: Class<*>,
      argument: Any? = null,
      timestamp: Instant = Instant.now(),
    ) : this(
      sourceNodeId = sourceNodeId,
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      argument = argument,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(
        proto: OnboardingProtos.ActivityNodeStartExecuteSynchronouslyProto,
        timestamp: Instant?,
      ) =
        ActivityNodeStartExecuteSynchronously(
          sourceNodeId = proto.sourceNodeId,
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          argument = null,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
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
    override val nodeName: String,
    override val nodeComponent: String,
    override val nodeId: Long,
    override val result: Any?,
    override val timestamp: Instant = Instant.now(),
  ) : OnboardingSynchronousActivityEvent, WithResult {
    constructor(
      nodeId: Long,
      nodeClass: Class<*>,
      result: Any?,
      timestamp: Instant = Instant.now(),
    ) : this(
      nodeId = nodeId,
      nodeName = OnboardingNode.extractNodeNameFromClass(nodeClass),
      nodeComponent = OnboardingNode.extractComponentNameFromClass(nodeClass),
      result = result,
      timestamp = timestamp,
    )

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

    companion object {
      fun fromProto(
        proto: OnboardingProtos.ActivityNodeExecutedSynchronouslyProto,
        timestamp: Instant?,
      ) =
        ActivityNodeExecutedSynchronously(
          nodeId = proto.nodeId,
          nodeName = proto.nodeName,
          nodeComponent = proto.nodeComponent,
          result = null,
          timestamp = timestamp ?: Instant.ofEpochMilli(proto.timestamp),
        )
    }
  }

  // LINT.ThenChange(//depot/google3/logs/proto/android_onboarding/node_interaction_metadata.proto)

  /**
   * Wrapper around the same information as [android.content.Intent] but can be used without
   * dependency on Android.
   */
  data class IntentData(val action: String?, val extras: Map<String, Any?>)

  companion object {
    fun deserialize(str: String, timestamp: Instant? = null): OnboardingEvent {
      val proto = OnboardingProtos.LogProto.parseFrom(BaseEncoding.base64().decode(str))

      return when {
        proto.hasActivityNodeExecutedDirectly() ->
          ActivityNodeExecutedDirectly.fromProto(proto.activityNodeExecutedDirectly, timestamp)
        proto.hasActivityNodeExecutedForResult() ->
          ActivityNodeExecutedForResult.fromProto(proto.activityNodeExecutedForResult, timestamp)
        proto.hasActivityNodeValidating() ->
          ActivityNodeValidating.fromProto(proto.activityNodeValidating, timestamp)
        proto.hasActivityNodeFailedValidation() ->
          ActivityNodeFailedValidation.fromProto(proto.activityNodeFailedValidation, timestamp)
        proto.hasActivityNodeExtractArgument() ->
          ActivityNodeExtractArgument.fromProto(proto.activityNodeExtractArgument, timestamp)
        proto.hasActivityNodeArgumentExtracted() ->
          ActivityNodeArgumentExtracted.fromProto(proto.activityNodeArgumentExtracted, timestamp)
        proto.hasActivityNodeSetResult() ->
          ActivityNodeSetResult.fromProto(proto.activityNodeSetResult, timestamp)
        proto.hasActivityNodeFail() -> ActivityNodeFail.fromProto(proto.activityNodeFail, timestamp)
        proto.hasActivityNodeResultReceived() ->
          ActivityNodeResultReceived.fromProto(proto.activityNodeResultReceived, timestamp)
        proto.hasActivityNodeStartExecuteSynchronously() ->
          ActivityNodeStartExecuteSynchronously.fromProto(
            proto.activityNodeStartExecuteSynchronously,
            timestamp,
          )
        proto.hasActivityNodeExecutedSynchronously() ->
          ActivityNodeExecutedSynchronously.fromProto(
            proto.activityNodeExecutedSynchronously,
            timestamp,
          )
        proto.hasActivityNodeFinished() ->
          ActivityNodeFinished.fromProto(proto.activityNodeFinished, timestamp)
        proto.hasActivityNodeResumedAfterLaunch() ->
          ActivityNodeResumedAfterLaunch.fromProto(proto.activityNodeResumedAfterLaunch, timestamp)
        proto.hasActivityNodeResumed() ->
          ActivityNodeResumed.fromProto(proto.activityNodeResumed, timestamp)
        else -> throw IllegalStateException("Could not parse $proto")
      }
    }
  }
}
