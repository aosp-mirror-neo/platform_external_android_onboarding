package com.android.onboarding.tasks

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.encoding.ResultHandler
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.nodes.UniqueNodeRef
import com.android.onboarding.nodes.generateHash
import com.android.onboarding.nodes.generateNodeId
import com.android.onboarding.nodes.nodeId
import com.android.onboarding.nodes.refersTo
import com.android.onboarding.pending.BackgroundLaunchable
import com.android.onboarding.pending.BackgroundLaunchableForResult
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.contentDeepEquals
import com.android.onboarding.utils.currentUserIdentifier
import com.android.onboarding.utils.generateHash
import com.android.onboarding.utils.persistable.NullSafePersistableBundleExtensions.getLongOrNull
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import java.time.Duration
import java.util.Objects
import java.util.function.Supplier

/**
 * Local-only variant of the [UnresolvedOnboardingTaskToken] preserving reference to the associated
 * contract. Note that it needs to be erased via [toErased] call in order for it to be passable
 * across process boundaries.
 *
 * @param timeout The timeout duration, after which, the node referenced by this token should be
 *   considered to have failed. Returns null if timeout is not applicable to the node.
 */
@OptIn(InternalOnboardingApi::class)
data class OnboardingTaskToken<O>(
  @OptIn(InternalOnboardingApi::class)
  @property:InternalOnboardingApi
  override val taskManagerId: Long,
  private val nodeRef: NodeRef,
  private val taskServiceIntentProvider: Supplier<Intent>,
  private val resultHandler: ResultHandler<PersistableBundle, O>,
  override val nodeId: NodeId,
  override val targetUserId: Int,
  override val targetPackageName: String,
  override val timeout: Duration? = null,
) :
  UnresolvedOnboardingTaskToken<O>,
  ResultHandler<PersistableBundle, O> by resultHandler,
  NodeRef by nodeRef {
  constructor(
    taskManagerId: Long,
    contract: BackgroundLaunchableForResult<*, O>,
    nodeId: NodeId = generateNodeId(),
    targetUserId: Int,
    targetPackageName: String,
    timeout: Duration? = null,
  ) : this(
    taskManagerId = taskManagerId,
    nodeRef = contract,
    taskServiceIntentProvider = Supplier { contract.taskServiceIntent.toIntent() },
    resultHandler = contract,
    nodeId = nodeId,
    targetUserId =
      targetUserId.let {
        if (it == ErasedOnboardingTaskToken.UNKNOWN_TARGET_USER_ID) currentUserIdentifier else it
      },
    targetPackageName =
      targetPackageName.let {
        if (it == ErasedOnboardingTaskToken.UNKNOWN_TARGET_PACKAGE_NAME) contract.nodePackage
        else it
      },
    timeout = timeout,
  )

  override fun equals(other: Any?): Boolean =
    other is OnboardingTaskToken<*> &&
      other.taskManagerId == taskManagerId &&
      other refersTo this &&
      other.targetUserId == targetUserId &&
      other.targetPackageName == targetPackageName

  override fun hashCode(): Int {
    return Objects.hash(
      taskManagerId,
      (this as UniqueNodeRef).generateHash(),
      targetUserId,
      targetPackageName,
    )
  }

  @VisibleForTesting
  val taskServiceIntent: Intent
    get() = taskServiceIntentProvider.get()

  @InternalOnboardingApi
  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().also { bundle ->
      bundle.nodeId = nodeId
      bundle.putString(EXTRA_COMPONENT, nodeComponent)
      bundle.putString(EXTRA_NODE, nodeName)
      bundle.putLong(ErasedOnboardingTaskToken.EXTRA_TASK_MANAGER_ID, taskManagerId)
      bundle.putInt(EXTRA_TARGET_USER, targetUserId)
      bundle.putString(EXTRA_TARGET_PACKAGE_NAME, targetPackageName)
      timeout?.let { bundle.putLong(EXTRA_TIMEOUT_IN_SECONDS, it.seconds) }
    }

  /**
   * Get the identity of the task definition for this token.
   *
   * This is equivalent to [identity] except that it does not include the reference to the specific
   * instance of the task (the node id).
   */
  val taskDefinitionIdentity
    get() = super<NodeRef>.identity()

  override fun identity(): String = super<UnresolvedOnboardingTaskToken>.identity()

  override fun toString(): String = "OnboardingTaskToken(${identity()})"

  companion object {
    const val NAMESPACE = "com.android.onboarding.task"
    const val EXTRA_COMPONENT = "$NAMESPACE.COMPONENT"
    const val EXTRA_NODE = "$NAMESPACE.NODE"
    const val SERVICE_ACTION = BackgroundLaunchable.SERVICE_ACTION
    const val EXTRA_TARGET_USER = "$NAMESPACE.TARGET_USER"
    const val EXTRA_TARGET_PACKAGE_NAME = "$NAMESPACE.TARGET_PACKAGE_NAME"
    const val EXTRA_TIMEOUT_IN_SECONDS = "$NAMESPACE.TIMEOUT_IN_SECONDS"
  }
}

/**
 * Represents a token to an onboarding task executor along with its associated contract.
 *
 * This is used to uniquely identify and reference an onboarding task executor and its contract.
 *
 * @param O associated task output type.
 */
sealed interface UnresolvedOnboardingTaskToken<out O> : UniqueNodeRef, Persistable, StableEquality {
  /** The ID of the [OnboardingTaskManager] */
  @InternalOnboardingApi val taskManagerId: Long

  /** The UUID associated with the node this task token represents. */
  override val nodeId: NodeId

  val targetUserId: Int
  val targetPackageName: String
  val timeout: Duration?

  /** Convert this token to a [PersistableBundle]. */
  @InternalOnboardingApi override fun toPersistableBundle(): PersistableBundle

  /**
   * Converts this token to [ErasedOnboardingTaskToken] token which loses contract reference in
   * order to become persistable and serializable.
   */
  @OptIn(InternalOnboardingApi::class)
  fun toErased(): ErasedOnboardingTaskToken = ErasedOnboardingTaskToken(toPersistableBundle())

  /**
   * Resolves this token to [OnboardingTaskToken] token by associating it with a given [contract].
   */
  @OptIn(InternalOnboardingApi::class)
  fun <OO> resolve(contract: BackgroundLaunchableForResult<*, OO>): OnboardingTaskToken<OO> =
    OnboardingTaskToken(
      taskManagerId = taskManagerId,
      contract = contract,
      nodeId = nodeId,
      targetUserId = targetUserId,
      targetPackageName = targetPackageName,
      timeout = timeout,
    )
}

/**
 * Erased variant of the [UnresolvedOnboardingTaskToken] which loses output type data in favour of
 * being persistable and serializable.
 */
class ErasedOnboardingTaskToken internal constructor(private val bundle: PersistableBundle) :
  UnresolvedOnboardingTaskToken<Nothing>, Parcelable by bundle {
  @InternalOnboardingApi
  override val taskManagerId: Long
    get() = bundle.getLong(EXTRA_TASK_MANAGER_ID, UNKNOWN_NODE_ID)

  override val nodeId: NodeId
    get() = bundle.nodeId

  override val nodeName: String
    get() = checkNotNull(bundle.getString(OnboardingTaskToken.EXTRA_NODE))

  override val nodeComponent: String
    get() = checkNotNull(bundle.getString(OnboardingTaskToken.EXTRA_COMPONENT))

  override val targetUserId: Int
    get() = bundle.getInt(OnboardingTaskToken.EXTRA_TARGET_USER, UNKNOWN_TARGET_USER_ID)

  override val targetPackageName: String
    get() =
      bundle.getString(OnboardingTaskToken.EXTRA_TARGET_PACKAGE_NAME) ?: UNKNOWN_TARGET_PACKAGE_NAME

  override val timeout: Duration?
    get() =
      bundle.getLongOrNull(OnboardingTaskToken.EXTRA_TIMEOUT_IN_SECONDS)?.let(Duration::ofSeconds)

  @OptIn(InternalOnboardingApi::class)
  override fun toPersistableBundle(): PersistableBundle = PersistableBundle(bundle)

  override fun toErased(): ErasedOnboardingTaskToken = this

  override fun equals(other: Any?) =
    other is ErasedOnboardingTaskToken && bundle contentDeepEquals other.bundle

  override fun hashCode() = bundle.generateHash()

  override fun toString(): String = "ErasedOnboardingTaskToken(${identity()})"

  companion object CREATOR :
    PersistableDeserializer<ErasedOnboardingTaskToken>,
    Parcelable.Creator<ErasedOnboardingTaskToken> {
    override fun createFromParcel(parcel: Parcel): ErasedOnboardingTaskToken =
      ErasedOnboardingTaskToken(PersistableBundle.CREATOR.createFromParcel(parcel))

    override fun newArray(size: Int): Array<ErasedOnboardingTaskToken?> = arrayOfNulls(size)

    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): ErasedOnboardingTaskToken = ErasedOnboardingTaskToken(persistableBundle)

    const val EXTRA_TASK_MANAGER_ID = "${OnboardingTaskToken.NAMESPACE}.TASK_MANAGER_ID"
    const val UNKNOWN_TARGET_PACKAGE_NAME = "unknown_target_package_name"
    const val UNKNOWN_TARGET_USER_ID = -1
  }
}
