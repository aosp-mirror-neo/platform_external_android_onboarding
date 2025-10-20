package com.android.onboarding.nodes

import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableSerializer
import java.util.Objects

/**
 * A [NodeRef] which is also unique within the scope of the same node kind. Usually represents an
 * instance of a given node kind.
 */
@OptIn(InternalOnboardingApi::class)
interface UniqueNodeRef : NodeRef, NodeAware {

  override fun identity(): String = "${super.identity()}${IDENTITY_SEPARATOR}$nodeId"

  /** Convert this [UniqueNodeRef] as an explicit [UniqueNodeRef] dropping potential overrides. */
  fun asUniqueNodeRef(): Stable = StableImpl(this)

  /** Ref with stable [hashCode] and [equals] implementations. */
  interface Stable : UniqueNodeRef, StableEquality, Persistable

  private data class StableImpl(
    override val nodeComponent: String,
    override val nodeName: String,
    override val nodeId: NodeId,
  ) : Stable {
    constructor(ref: UniqueNodeRef) : this(ref.nodeComponent, ref.nodeName, ref.nodeId)

    override fun toPersistableBundle() = toPersistableBundle(this)

    override fun toString(): String = identity()

    override fun asUniqueNodeRef(): Stable = this
  }

  companion object : PersistableDeserializer<Stable>, PersistableSerializer<UniqueNodeRef> {
    private const val KEY_NODE_ID = "unr.NODE_ID"

    override fun toPersistableBundle(value: UniqueNodeRef) =
      NodeRef.toPersistableBundle(value).apply { putLong(KEY_NODE_ID, value.nodeId) }

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      UniqueNodeRef(
        node = NodeRef.fromPersistableBundle(persistableBundle),
        nodeId = requireNotNull(persistableBundle.getLong(KEY_NODE_ID, UNKNOWN_NODE_ID)),
      )

    /** Create a [UniqueNodeRef] for a specific [node] instance. */
    @InternalOnboardingApi
    @JvmName("of")
    operator fun invoke(node: NodeRef, nodeId: NodeId): Stable =
      StableImpl(nodeComponent = node.nodeComponent, nodeName = node.nodeName, nodeId = nodeId)

    /**
     * Create a [NodeRef] for the given identity.
     *
     * The identity must match that produced by [identity].
     */
    @InternalOnboardingApi
    @JvmName("of")
    operator fun invoke(identity: String): Stable =
      identity.split(IDENTITY_SEPARATOR).let {
        if (it.size != 3) error("Invalid identity $it (not a UniqueNodeRef)")
        else StableImpl(it[0], it[1], it[2].toLong())
      }
  }
}

/** Compares this [UniqueNodeRef] with [other] and returns true if both reference the same node. */
infix fun UniqueNodeRef.refersTo(other: UniqueNodeRef): Boolean =
  (this as NodeRef) refersTo other && nodeId == other.nodeId

/** Generate a hash code. */
fun UniqueNodeRef.generateHash() = Objects.hash((this as NodeRef).generateHash(), nodeId)
