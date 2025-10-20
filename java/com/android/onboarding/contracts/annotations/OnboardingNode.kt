@file:OptIn(InternalOnboardingApi::class)

package com.android.onboarding.contracts.annotations

import com.android.onboarding.metadata.OnboardingMetadata
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableEnum
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotation
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotation.ExtractMetadata
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotationCompanionDelegate
import java.time.Duration
import kotlin.reflect.KClass

/** Container of metadata about a node definition in the onboarding graph */
@Retention(AnnotationRetention.RUNTIME)
@PersistableAnnotation(lossy = true)
annotation class OnboardingNode(
  /** Identifier of the component who owns the node. */
  val component: String,

  /**
   * Identifier of the node interface.
   *
   * Note that this must be at most 40 characters long, otherwise an exception will be thrown.
   */
  val name: String,

  /**
   * Node data specific to foreground nodes.
   *
   * This is mandatory for foreground nodes and must NOT be provided for background nodes.
   */
  // All values here except "empty" are arbitrary as this just marks that the data hasn't been
  // provided
  val foreground: ForegroundOnboardingNode =
    ForegroundOnboardingNode(uiType = ForegroundOnboardingNode.UiType.INVISIBLE, empty = true),

  /**
   * Node data specific to background nodes.
   *
   * This is mandatory for background nodes and must NOT be provided for foreground nodes.
   */
  // All values here except "empty" are arbitrary as this just marks that the data hasn't been
  // provided
  val background: BackgroundOnboardingNode = BackgroundOnboardingNode(empty = true),

  /** The type of specification given for this node. */
  val specificationType: SpecificationType = SpecificationType.LIGHT,

  /** The packages which are expected to launch this node without making use of the contract. */
  // Once we need it - we can add constants representing categories of caller that can be
  // special cased. For example, DPCs or Settings apps.
  val offGraphCallers: Array<String> = [],

  /**
   * True if this node is expected to terminate the Onboarding flow in some circumstances.
   *
   * This is only applicable to UI Nodes.
   *
   * No node with this set to false should exit without returning a result or starting another
   * contract which continues the flow.
   */
  // We should refactor this into the UiType option somehow so it can't be specified on non-UI nodes
  // or at least we should enforce it in the linter
  val isTerminalNode: Boolean = false,

  /** The time in seconds, after which, this node should be considered to have failed. */
  val timeoutInSeconds: Long = DEFAULT_TIMEOUT_IN_SECONDS,

  /**
   * Metadata which is supported by this node.
   *
   * Nodes of this type may only expose metadata listed here. It is not guaranteed that they will
   * expose all listed metadata.
   */
  @get:ExtractMetadata val metadata: Array<KClass<out OnboardingMetadata>> = [],
) {
  enum class SpecificationType : PersistableEnum {
    /**
     * No requirements.
     *
     * Most "Light" nodes will be lacking documentation, and using Intents and Bundles as arguments
     * and return values.
     */
    LIGHT,

    /**
     * This means that full Javadoc is provided, all arguments are fully defined, documented and
     * typed (no undefined Bundles or Intents), and return values are properly defined and typed.
     */
    BASELINE,

    /**
     * Same as [BASELINE] except that we have confidence in our contract completeness so can enforce
     * strict validation
     */
    V1;

    override val id = name

    companion object :
      PersistableDeserializer<SpecificationType> by PersistableEnum.deserializer(entries, LIGHT)
  }

  companion object :
    PersistableAnnotationCompanionDelegate<OnboardingNode, POnboardingNode>(POnboardingNode) {
    /** Validates a given node [name]. */
    fun validateNodeName(name: String) {
      require(name.length <= MAX_NODE_NAME_LENGTH) {
        "Node name length (${name.length}) exceeds maximum length of $MAX_NODE_NAME_LENGTH characters"
      }
    }

    /** Validates a given node [timeoutInSeconds]. */
    fun validateNodeTimeout(timeoutInSeconds: Long) {
      require(timeoutInSeconds > 0) { "Node timeout ($timeoutInSeconds) must be a positive Long" }
    }

    /** Returns the node [OnboardingNode] metadata for given node's contract class. */
    fun extractNodeMetadata(nodeClass: Class<*>): OnboardingNode =
      extractNodeMetadataOrNull(nodeClass)
        ?: throw IllegalArgumentException("All nodes must be annotated @OnboardingNode")

    /** Returns the node [OnboardingNode] metadata for given node's contract class. */
    fun extractNodeMetadataOrNull(nodeClass: Class<*>): OnboardingNode? =
      nodeClass.getAnnotation(OnboardingNode::class.java)

    /** Returns the node [component] for given node's contract class. */
    fun extractComponentNameFromClass(nodeClass: Class<*>): String =
      extractNodeMetadata(nodeClass).component

    /** Returns the node [name] for given node's contract class. */
    fun extractNodeNameFromClass(nodeClass: Class<*>): String =
      extractNodeMetadata(nodeClass).name.also(::validateNodeName)

    /** Returns the node [timeoutInSeconds] for given node's contract class. */
    fun extractTimeoutDurationFromClass(nodeClass: Class<*>): Duration =
      extractNodeMetadata(nodeClass).timeoutInSeconds.also(::validateNodeTimeout).let {
        Duration.ofSeconds(it)
      }

    /** Returns the node [OnboardingNodeMetadata] for given node's component name. */
    fun getOnboardingNodeMetadata(nodeClass: Class<*>): OnboardingNodeMetadata {
      val component = extractNodeMetadata(nodeClass).component
      val nodeMetadata = component.split("/")
      return when (nodeMetadata.size) {
        1 -> OnboardingNodeMetadata(nodeMetadata[0], nodeMetadata[0])
        2 -> OnboardingNodeMetadata(nodeMetadata[0], nodeMetadata[1])
        else -> error("OnboardingNode component $component is invalid")
      }
    }
  }
}

/** Extract [NodeRef] from this [OnboardingNode]. */
fun OnboardingNode.asNodeRef(): NodeRef.Stable = NodeRef(ComponentRef(component), name)

const val MAX_NODE_NAME_LENGTH: Int = 40

const val DEFAULT_TIMEOUT_IN_SECONDS: Long = 300L
