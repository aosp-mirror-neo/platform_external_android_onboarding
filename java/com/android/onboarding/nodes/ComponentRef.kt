package com.android.onboarding.nodes

import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableBundle
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableSerializer

/** Interface which represents an Onboarding Component. */
@OptIn(InternalOnboardingApi::class)
interface ComponentRef {
  /**
   * The component constant this refers to.
   *
   * This will match the package name of the component, optionally followed by a '/' and then the
   * 'subcomponent' if the component has a subcomponent. A subcomponent is only needed when a single
   * package contains multiple components.
   */
  val nodeComponent: String

  /** Android package name for this component */
  val nodePackage: String
    get() = nodeComponent.substringBefore("/")

  /** Subcomponent for this component */
  val subcomponent: String
    get() =
      nodeComponent
        .substringAfterLast("/", missingDelimiterValue = "")
        .takeIf(String::isNotBlank)
        .let(::checkNotNull)

  /** Returns a string representation of this ref that identifies it. */
  fun identity(): String = nodeComponent

  /**
   * Convert this [ComponentRef] as an explicit [ComponentRef] dropping potential overrides and
   * providing stable [equals] and [hashCode] implementations.
   */
  fun asComponentRef(): Stable = StableImpl(this)

  /** Ref with stable [hashCode] and [equals] implementations. */
  sealed interface Stable : ComponentRef, StableEquality, Persistable

  private data class StableImpl(override val nodeComponent: String) : Stable {
    constructor(ref: ComponentRef) : this(ref.nodeComponent)

    override fun toPersistableBundle() = toPersistableBundle(this)

    override fun toString(): String = identity()

    override fun asComponentRef(): Stable = this
  }

  companion object : PersistableDeserializer<Stable>, PersistableSerializer<ComponentRef> {
    private const val KEY_NODE_COMPONENT = "cr.KEY_NODE_COMPONENT"

    override fun toPersistableBundle(value: ComponentRef) = PersistableBundle {
      putString(KEY_NODE_COMPONENT, value.nodeComponent)
    }

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      ComponentRef(requireNotNull(value = persistableBundle.getString(KEY_NODE_COMPONENT)))

    /**
     * Create a [ComponentRef] for the given [nodeComponent].
     *
     * This should only be used with OnboardingNode constants.
     */
    @InternalOnboardingApi
    @JvmName("of")
    operator fun invoke(nodeComponent: String): Stable =
      nodeComponent.split(IDENTITY_SEPARATOR).let {
        if (it.isEmpty()) error("Invalid identity $it (not a ComponentRef)") else StableImpl(it[0])
      }
  }
}

/** Compares this [ComponentRef] with [other] and returns true if both reference the same node. */
infix fun ComponentRef.refersTo(other: ComponentRef): Boolean = nodeComponent == other.nodeComponent

/** Generate a hash code. */
fun ComponentRef.generateHash() = nodeComponent.hashCode()

/**
 * Separator used to split various identity chunks in serialized form of [ComponentRef.identity].
 */
const val IDENTITY_SEPARATOR = ":"
