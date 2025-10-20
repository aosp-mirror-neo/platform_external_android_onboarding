package com.android.onboarding.nodes

import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableSerializer
import java.util.Objects

/**
 * Interface which represents an Onboarding Node.
 *
 * Specifically, this refers to the definition of the node (e.g. the class) not a specific instance
 * of the node (which has a node ID and is represented by [UniqueNodeRef]).
 */
@OptIn(InternalOnboardingApi::class)
interface NodeRef : ComponentRef {
  /** The node name (specified in the "name" attribute on contracts) this is referring to. */
  val nodeName: String

  override fun identity(): String = "${super.identity()}${IDENTITY_SEPARATOR}$nodeName"

  /** Convert this [NodeRef] as an explicit [NodeRef] dropping potential overrides. */
  fun asNodeRef(): Stable = StableImpl(this)

  /** Ref with stable [hashCode] and [equals] implementations. */
  sealed interface Stable : NodeRef, StableEquality, Persistable

  private data class StableImpl(override val nodeComponent: String, override val nodeName: String) :
    Stable {
    constructor(nodeRef: NodeRef) : this(nodeRef.nodeComponent, nodeRef.nodeName)

    override fun toPersistableBundle() = toPersistableBundle(this)

    override fun toString(): String = identity()

    override fun asNodeRef(): Stable = this
  }

  companion object : PersistableDeserializer<Stable>, PersistableSerializer<NodeRef> {
    private const val KEY_NODE_NAME = "nr.NODE_NAME"

    override fun toPersistableBundle(value: NodeRef) =
      ComponentRef.toPersistableBundle(value).apply { putString(KEY_NODE_NAME, value.nodeName) }

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      NodeRef(
        component = ComponentRef.fromPersistableBundle(persistableBundle),
        nodeName = requireNotNull(persistableBundle.getString(KEY_NODE_NAME)),
      )

    /** Create a [NodeRef] for a specific node of the given [component]. */
    @InternalOnboardingApi
    @JvmName("of")
    operator fun invoke(component: ComponentRef, nodeName: String): Stable =
      StableImpl(component.nodeComponent, nodeName)

    /**
     * Create a [NodeRef] for the given identity.
     *
     * The identity must match that produced by [identity].
     */
    @InternalOnboardingApi
    @JvmName("of")
    operator fun invoke(identity: String): Stable =
      identity.split(IDENTITY_SEPARATOR).let {
        if (it.size < 2) error("Invalid identity $it (not a nodeRef)") else StableImpl(it[0], it[1])
      }
  }
}

/**
 * Compares this [NodeRef] with [other] and returns true if both reference the same node.
 *
 * Referencing the same node means that they belong to the same component and have the same node
 * name. Other details (such as node instance IDs) are ignored.
 */
infix fun NodeRef.refersTo(other: NodeRef): Boolean =
  (this as ComponentRef) refersTo other && nodeName == other.nodeName

/** Generate a hash code. */
fun NodeRef.generateHash() = Objects.hash((this as ComponentRef).generateHash(), nodeName)
