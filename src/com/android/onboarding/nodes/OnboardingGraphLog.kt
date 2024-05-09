package com.android.onboarding.nodes

import com.android.onboarding.OnboardingProtos
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.google.common.io.BaseEncoding
import java.time.Instant

/**
 * A target for onboarding logs which can register observers to store those logs.
 *
 * Note that this will only include part of the overall graph, and must be linked with the graph
 * from other onboarding components to see a complete view.
 */
interface OnboardingGraphLog {

  /** Add an observer for future graph changes. */
  fun addObserver(observer: Observer)

  /** Remove the observer so it stops receiving updates. */
  fun removeObserver(observer: Observer)

  /**
   * Log an Onboarding event.
   *
   * This will be propagated to all observers.
   */
  fun log(event: OnboardingEvent)

  /** Interface to implement to observe [OnboardingEvent] instances. */
  interface Observer {
    /** Called when an [OnboardingEvent] occurs. */
    fun onEvent(event: OnboardingEvent)
  }

  interface OnboardingEventData {
    val nodeName: String?
    val nodeComponent: String?
    val nodeId: Long
    val timestamp: Instant
  }

  // LINT.IfChange
  /** Possible Events. */
  sealed class OnboardingEvent(
    override val nodeName: String?,
    override val nodeComponent: String?,
    override val nodeId: Long,
  ) : OnboardingEventData {
    /** Writes this event to a string which can be parsed by [OnboardingGraphLog#deserialize]. */
    fun serializeToString(): String {
      return BaseEncoding.base64().encode(serialize().toByteArray())
    }

    abstract fun serialize(): OnboardingProtos.LogProto

    // Activities

    /**
     * At [timestamp], an [android.app.Activity] with id [nodeId] has been launched by invoking the
     * [nodeName] contract from inside the [sourceNodeId] node with [argument] without expecting a
     * result.
     */
    data class ActivityNodeExecutedDirectly
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      val sourceNodeId: Long,
      override val nodeId: Long,
      val argument: Any? = null,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeName = nodeName, nodeId = nodeId, nodeComponent = nodeComponent) {

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
          )
      }
    }

    /**
     * At [timestamp], an [android.app.Activity] with id [nodeId] has been launched by invoking the
     * [nodeName] contract from inside the [sourceNodeId] node with [argument] while expecting a
     * result.
     */
    data class ActivityNodeExecutedForResult
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      val sourceNodeId: Long,
      override val nodeId: Long,
      val argument: Any? = null,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {

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
     * At [timestamp], an [android.app.Activity] with id [nodeId] is using the [nodeName] contract
     * to validate intent [intent].
     */
    data class ActivityNodeValidating
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      override val nodeId: Long,
      val intent: IntentData? = null,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {

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
     */
    data class ActivityNodeFailedValidation
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      override val nodeId: Long,
      val exception: Throwable = IllegalArgumentException("Failed validation"),
      val intent: IntentData? = null,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {

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
     */
    data class ActivityNodeExtractArgument
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      override val nodeId: Long,
      val intent: IntentData,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {

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
        fun fromProto(
          proto: OnboardingProtos.ActivityNodeExtractArgumentProto,
          timestamp: Instant?,
        ) =
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
     */
    data class ActivityNodeArgumentExtracted
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      override val nodeId: Long,
      val argument: Any?,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {

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
     * At [timestamp], an [android.app.Activity] with id [nodeId] has set the result to [result]
     * using the [nodeName] contract.
     */
    data class ActivityNodeSetResult
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      override val nodeId: Long,
      val result: Any?,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {

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
     */
    data class ActivityNodeFail(
      override val nodeId: Long,
      val reason: String?,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = null, nodeComponent = null) {

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
     * At [timestamp], the [result] of the [android.app.Activity] node with id [nodeId] was
     * received.
     *
     * Note that this does not specify which node received the result. The event must be matched up
     * with a previous [ActivityNodeExecutedForResult] event.
     */
    data class ActivityNodeResultReceived
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      override val nodeId: Long,
      val result: Any?,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {

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
        fun fromProto(
          proto: OnboardingProtos.ActivityNodeResultReceivedProto,
          timestamp: Instant?,
        ) =
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
     * At [timestamp], the node with id [sourceNodeId] began synchronous execution of the node with
     * id [nodeId].
     */
    data class ActivityNodeStartExecuteSynchronously
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      val sourceNodeId: Long,
      override val nodeId: Long,
      val argument: Any?,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {

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

    /** At [timestamp], the synchronously executing node with id [nodeId] got result [result]. */
    data class ActivityNodeExecutedSynchronously
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      override val nodeId: Long,
      val result: Any?,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {

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

    /** At [timestamp], the activity node with id [nodeId] called finish. */
    data class ActivityNodeFinished
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      override val nodeId: Long,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {
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
     */
    data class ActivityNodeResumedAfterLaunch
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      val sourceNodeId: Long,
      override val nodeId: Long,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {
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

    /** At [timestamp], the activity node with id [nodeId] called finish. */
    data class ActivityNodeResumed
    private constructor(
      override val nodeName: String,
      override val nodeComponent: String,
      override val nodeId: Long,
      override val timestamp: Instant = Instant.now(),
    ) : OnboardingEvent(nodeId = nodeId, nodeName = nodeName, nodeComponent = nodeComponent) {
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
          proto.hasActivityNodeFail() ->
            ActivityNodeFail.fromProto(proto.activityNodeFail, timestamp)
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
            ActivityNodeResumedAfterLaunch.fromProto(
              proto.activityNodeResumedAfterLaunch,
              timestamp,
            )
          proto.hasActivityNodeResumed() ->
            ActivityNodeResumed.fromProto(proto.activityNodeResumed, timestamp)
          else -> throw IllegalStateException("Could not parse $proto")
        }
      }
    }
  }
  // LINT.ThenChange(//depot/google3/logs/proto/android_onboarding/node_interaction_metadata.proto)
}
