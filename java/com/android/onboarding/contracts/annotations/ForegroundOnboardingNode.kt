package com.android.onboarding.contracts.annotations

import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableEnum
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotation
import com.android.onboarding.utils.persistable.annotations.PersistableAnnotationCompanionDelegate

/** Additional [OnboardingNode] annotation data used for nodes with a UI. */
@Retention(AnnotationRetention.RUNTIME)
@Target()
@OptIn(InternalOnboardingApi::class)
@PersistableAnnotation
annotation class ForegroundOnboardingNode(
  /** The type of UI shown as part of this node. */
  val uiType: UiType,

  /** How the back button behaves when this node is active. */
  val backButtonBehavior: BackButtonBehavior = BackButtonBehavior.UNKNOWN,

  /** Value used only by infrastructure. This should not be changed by an individual node. */
  val empty: Boolean = false,
) {
  enum class BackButtonBehavior : PersistableEnum {
    UNKNOWN,

    /**
     * The back button is disabled all of the time.
     *
     * This will be automatically enforced as long as contracts are correctly used.
     */
    DISABLED,

    /**
     * Disabled via some screen-specific logic.
     *
     * Components are responsible for implementing the actual disablement.
     */
    CUSTOM_DISABLED;

    override val id = name

    companion object :
      PersistableDeserializer<BackButtonBehavior> by PersistableEnum.deserializer(entries, UNKNOWN)
  }

  enum class UiType : PersistableEnum {
    /** This node shows UI which does not fit any other type. */
    @Deprecated("Open a bug to create a new category rather than using this") OTHER,

    /**
     * This node does not actually show UI but makes use of UI controls as a way of passing control.
     * This will be replaced by some other mechanism in future.
     */
    @Deprecated(
      "Do not create new invisible UIs. Use http://___ to discuss alternative options"
    )
    INVISIBLE,

    /**
     * This node shows a loading screen. The primary purpose is to control the screen while some
     * background work executes.
     */
    @Deprecated(
      "Do not create new loading screens. Use http://___ to discuss alternative options"
    )
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

    /** Internal-use only to indicate parsing errors. */
    @InternalOnboardingApi UNKNOWN;

    override val id = name

    companion object :
      PersistableDeserializer<UiType> by PersistableEnum.deserializer(entries, UNKNOWN)
  }

  companion object :
    PersistableAnnotationCompanionDelegate<ForegroundOnboardingNode, PForegroundOnboardingNode>(
      PForegroundOnboardingNode
    )
}
