package com.android.onboarding.nodes

import com.android.onboarding.OnboardingProtos
import com.google.common.io.BaseEncoding
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.time.Instant

/** Extra key used when storing a node id */
const val EXTRA_ONBOARDING_NODE_ID = "com.android.onboarding.ONBOARDING_NODE_ID"

/**
 * A target for onboarding logs which can register observers to store those logs.
 *
 * Note that this will only include part of the overall graph, and must be linked with the graph
 * from other onboarding components to see a complete view.
 */
interface OnboardingGraphLog {

  /**
   * Add an observer for future graph changes.
   *
   * This is stored weakly, so will only report results as long as the observer is not garbage
   * collected.
   */
  fun addObserver(observer: Observer)

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

  // LINT.IfChange
  /** Possible Events. */
  sealed class OnboardingEvent {

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
    data class ActivityNodeExecutedDirectly(
      val sourceNodeId: Long,
      val nodeId: Long,
      val nodeName: String,
      val argument: Any? = null,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeExecutedDirectly(
            OnboardingProtos.ActivityNodeExecutedDirectlyProto.newBuilder()
              .setSourceNodeId(sourceNodeId)
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              // .setArgument(argument)
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeExecutedDirectlyProto) =
          ActivityNodeExecutedDirectly(
            sourceNodeId = proto.sourceNodeId,
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /**
     * At [timestamp], an [android.app.Activity] with id [nodeId] has been launched by invoking the
     * [nodeName] contract from inside the [sourceNodeId] node with [argument] while expecting a
     * result.
     */
    data class ActivityNodeExecutedForResult(
      val sourceNodeId: Long,
      val nodeId: Long,
      val nodeName: String,
      val argument: Any? = null,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeExecutedForResult(
            OnboardingProtos.ActivityNodeExecutedForResultProto.newBuilder()
              .setSourceNodeId(sourceNodeId)
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              // .setArgument(argument)
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeExecutedForResultProto) =
          ActivityNodeExecutedForResult(
            sourceNodeId = proto.sourceNodeId,
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /**
     * At [timestamp], an [android.app.Activity] with id [nodeId] is using the [nodeName] contract
     * to validate intent [intent].
     */
    data class ActivityNodeValidating(
      val nodeId: Long,
      val nodeName: String,
      val intent: IntentData? = null,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeValidating(
            OnboardingProtos.ActivityNodeValidatingProto.newBuilder()
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              // .setIntent(intent)
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeValidatingProto) =
          ActivityNodeValidating(
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /**
     * At [timestamp], an [android.app.Activity] with id [nodeId] has failed validation of [intent]
     * using the [nodeName] contract. The exception was [exception].
     */
    data class ActivityNodeFailedValidation(
      val nodeId: Long,
      val nodeName: String,
      val exception: Exception = IllegalArgumentException("Failed validation"),
      val intent: IntentData? = null,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeFailedValidation(
            OnboardingProtos.ActivityNodeFailedValidationProto.newBuilder()
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              // .setException(exception)
              // .setIntent(IntentData(intent?.action ?: "", mapOf()))
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeFailedValidationProto) =
          ActivityNodeFailedValidation(
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /**
     * At [timestamp], an [android.app.Activity] with id [nodeId] is extracting the argument from
     * [intent] using the [nodeName] contract.
     */
    data class ActivityNodeExtractArgument(
      val nodeId: Long,
      val nodeName: String,
      val intent: IntentData,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeExtractArgument(
            OnboardingProtos.ActivityNodeExtractArgumentProto.newBuilder()
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              .setIntent(
                OnboardingProtos.IntentDataProto.newBuilder().setAction(intent.action ?: "").build()
              )
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeExtractArgumentProto) =
          ActivityNodeExtractArgument(
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            intent =
              IntentData(
                if (proto.hasIntent()) proto.intent.action else "",
                mapOf()
              ),
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /**
     * At [timestamp], an [android.app.Activity] with id [nodeId] has extracted [argument] using the
     * [nodeName] contract.
     */
    data class ActivityNodeArgumentExtracted(
      val nodeId: Long,
      val nodeName: String,
      val argument: Any?,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeArgumentExtracted(
            OnboardingProtos.ActivityNodeArgumentExtractedProto.newBuilder()
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              // setArgument(argument)
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeArgumentExtractedProto) =
          ActivityNodeArgumentExtracted(
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            argument = null,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /**
     * At [timestamp], an [android.app.Activity] with id [nodeId] has set the result to [result]
     * using the [nodeName] contract.
     */
    data class ActivityNodeSetResult(
      val nodeId: Long,
      val nodeName: String,
      val result: Any?,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeSetResult(
            OnboardingProtos.ActivityNodeSetResultProto.newBuilder()
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              // setResult(result)
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeSetResultProto) =
          ActivityNodeSetResult(
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            result = null,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /**
     * At [timestamp], an [android.app.Activity] with id [nodeId] has failed the [nodeName] contract
     * because of [reason].
     */
    data class ActivityNodeFail(
      val nodeId: Long,
      val nodeName: String,
      val reason: String?,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeFail(
            OnboardingProtos.ActivityNodeFailProto.newBuilder()
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              .setReason(reason ?: "")
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeFailProto) =
          ActivityNodeFail(
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            reason = proto.reason,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
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
    data class ActivityNodeResultReceived(
      val nodeId: Long,
      val nodeName: String,
      val result: Any?,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeResultReceived(
            OnboardingProtos.ActivityNodeResultReceivedProto.newBuilder()
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              // setResult(result)
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeResultReceivedProto) =
          ActivityNodeResultReceived(
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            result = null,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /**
     * At [timestamp], the node with id [sourceNodeId] began synchronous execution of the node with
     * id [nodeId].
     */
    data class ActivityNodeStartExecuteSynchronously(
      val sourceNodeId: Long,
      val nodeId: Long,
      val nodeName: String,
      val argument: Any?,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeStartExecuteSynchronously(
            OnboardingProtos.ActivityNodeStartExecuteSynchronouslyProto.newBuilder()
              .setSourceNodeId(sourceNodeId)
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              // setArgument(argument)
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeStartExecuteSynchronouslyProto) =
          ActivityNodeStartExecuteSynchronously(
            sourceNodeId = proto.sourceNodeId,
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            argument = null,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /** At [timestamp], the synchronously executing node with id [nodeId] got result [result]. */
    data class ActivityNodeExecutedSynchronously(
      val nodeId: Long,
      val nodeName: String,
      val result: Any?,
      val timestamp: Instant = Instant.now()
    ) : OnboardingEvent() {
      override fun serialize(): OnboardingProtos.LogProto {
        return OnboardingProtos.LogProto.newBuilder()
          .setActivityNodeExecutedSynchronously(
            OnboardingProtos.ActivityNodeExecutedSynchronouslyProto.newBuilder()
              .setNodeId(nodeId)
              .setNodeName(nodeName)
              // setResult(result)
              .setTimestamp(timestamp.toEpochMilli())
              .build()
          )
          .build()
      }

      companion object {
        fun fromProto(proto: OnboardingProtos.ActivityNodeExecutedSynchronouslyProto) =
          ActivityNodeExecutedSynchronously(
            nodeId = proto.nodeId,
            nodeName = proto.nodeName,
            result = null,
            timestamp = Instant.ofEpochMilli(proto.timestamp)
          )
      }
    }

    /**
     * Wrapper around the same information as [android.content.Intent] but can be used without
     * dependency on Android.
     */
    data class IntentData(val action: String?, val extras: Map<String, Any?>)

    companion object {
      fun deserialize(str: String): OnboardingEvent {
        val proto = OnboardingProtos.LogProto.parseFrom(BaseEncoding.base64().decode(str))

        if (proto.hasActivityNodeExecutedDirectly()) {
          return ActivityNodeExecutedDirectly.fromProto(proto.activityNodeExecutedDirectly)
        } else if (proto.hasActivityNodeExecutedForResult()) {
          return ActivityNodeExecutedForResult.fromProto(proto.activityNodeExecutedForResult)
        } else if (proto.hasActivityNodeValidating()) {
          return ActivityNodeValidating.fromProto(proto.activityNodeValidating)
        } else if (proto.hasActivityNodeFailedValidation()) {
          return ActivityNodeFailedValidation.fromProto(proto.activityNodeFailedValidation)
        } else if (proto.hasActivityNodeExtractArgument()) {
          return ActivityNodeExtractArgument.fromProto(proto.activityNodeExtractArgument)
        } else if (proto.hasActivityNodeArgumentExtracted()) {
          return ActivityNodeArgumentExtracted.fromProto(proto.activityNodeArgumentExtracted)
        } else if (proto.hasActivityNodeSetResult()) {
          return ActivityNodeSetResult.fromProto(proto.activityNodeSetResult)
        } else if (proto.hasActivityNodeFail()) {
          return ActivityNodeFail.fromProto(proto.activityNodeFail)
        } else if (proto.hasActivityNodeResultReceived()) {
          return ActivityNodeResultReceived.fromProto(proto.activityNodeResultReceived)
        } else if (proto.hasActivityNodeStartExecuteSynchronously()) {
          return ActivityNodeStartExecuteSynchronously.fromProto(
            proto.activityNodeStartExecuteSynchronously
          )
        } else if (proto.hasActivityNodeExecutedSynchronously()) {
          return ActivityNodeExecutedSynchronously.fromProto(
            proto.activityNodeExecutedSynchronously
          )
        }

        throw IllegalStateException("Could not parse $proto")
      }
    }
  }
  // LINT.ThenChange(//depot/google3/logs/proto/android_onboarding/node_interaction_metadata.proto)
}
