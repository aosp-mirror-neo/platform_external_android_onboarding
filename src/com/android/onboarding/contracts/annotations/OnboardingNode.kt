package com.android.onboarding.contracts.annotations

/** Container of metadata about a node definition in the onboarding graph */
@Retention(AnnotationRetention.RUNTIME)
annotation class OnboardingNode(
  /** Identifier of the component who owns the node. */
  val component: String,

  /**
   * Identifier of the node interface.
   *
   * Note that this must be at most 40 characters long, otherwise an exception will be thrown.
   */
  val name: String,

  /** The type of UI shown as part of this node. */
  val uiType: UiType,

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

  /**
   * How the back button behaves when this node is active.
   *
   * This is only applicable to UI Nodes.
   */
  // We should refactor this into the UiType option somehow so it can't be specified on non-UI nodes
  // or at least we should enforce it in the linter
  val backButtonBehavior: BackButtonBehavior = BackButtonBehavior.UNKNOWN,
) {

  enum class BackButtonBehavior {
    UNKNOWN,

    /** Disabled via some screen-specific logic. */
    CUSTOM_DISABLED,
  }

  enum class UiType {
    /** This node shows no UI. */
    NONE,

    /** This node shows UI which does not fit any other type. */
    OTHER,

    /**
     * This node does not actually show UI but makes use of UI controls as a way of passing control.
     * This will be replaced by some other mechanism in future.
     */
    INVISIBLE,

    /**
     * This node shows a loading screen. The primary purpose is to control the screen while some
     * background work executes.
     */
    LOADING,

    /**
     * This node shows an education screen. The primary purpose is to educate the user about their
     * device, privacy, etc.
     */
    EDUCATION,

    /** This node's primary purpose is to input a decision or some data from the user. */
    INPUT,

    /**
     * This node's primary purpose is to host some other UI node. This node must not show UI itself
     * independently of another node.
     *
     * For example, an Activity which hosts fragments, where those fragments themselves are nodes,
     * could be marked as [HOST].
     */
    HOST,

    /** This node's primary purpose is to show error screen to the user. */
    ERROR,
  }

  enum class SpecificationType {
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
    V1,
  }

  companion object {
    /** Returns the node [component] for given node's contract class. */
    fun extractComponentNameFromClass(nodeClass: Class<*>): String =
      nodeClass.getAnnotation(OnboardingNode::class.java)?.component
        ?: throw IllegalArgumentException("All nodes must be annotated @OnboardingNode")

    /** Returns the node [name] for given node's contract class. */
    fun extractNodeNameFromClass(nodeClass: Class<*>): String =
      nodeClass.getAnnotation(OnboardingNode::class.java)?.name?.also {
        require(it.length <= MAX_NODE_NAME_LENGTH) {
          "Node name length (${it.length}) exceeds maximum length of $MAX_NODE_NAME_LENGTH characters"
        }
      } ?: throw IllegalArgumentException("All nodes must be annotated @OnboardingNode")

    /** Returns the node [OnboardingNodeMetadata] for given node's component name. */
    fun getOnboardingNodeMetadata(nodeClass: Class<*>): OnboardingNodeMetadata {
      val component =
        nodeClass.getAnnotation(OnboardingNode::class.java)?.component
          ?: error("OnboardingNode annotation or component is missing for class ${nodeClass.name}")
      val nodeMetadata = component.split("/")
      return when (nodeMetadata.size) {
        1 -> OnboardingNodeMetadata(nodeMetadata[0], nodeMetadata[0])
        2 -> OnboardingNodeMetadata(nodeMetadata[0], nodeMetadata[1])
        else -> error("OnboardingNode component ${component} is invalid")
      }
    }
  }
}

const val MAX_NODE_NAME_LENGTH: Int = 40
