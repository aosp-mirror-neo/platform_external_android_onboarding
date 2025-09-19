package com.android.onboarding.pending

import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import android.os.Process
import androidx.annotation.VisibleForTesting
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.encoding.Encoder
import com.android.onboarding.encoding.ResultHandler
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.nodes.nodeId
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_COMPONENT
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_CREATOR_ID
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_LAUNCH_ID
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_NODE
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_SERVICE_INTENT
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_SOURCE_ID
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_TARGET_PACKAGE_NAME
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_TARGET_USER_ID
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_TYPE
import com.android.onboarding.pending.PendingContractMetadata.Companion.NAMESPACE
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.contentDeepEquals
import com.android.onboarding.utils.generateHash
import com.android.onboarding.utils.persistable.PersistableIntent
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable

@InternalOnboardingApi
class PendingTaskContract(

  @property:VisibleForTesting val bundle: PersistableBundle
) : PendingContract.ErasedTask, Parcelable by bundle, PendingContractMetadata {
  constructor(
    input: PersistableBundle,
    ref: NodeRef,
    creatorNodeId: NodeId,
    taskServiceIntent: PersistableIntent,
    targetUserId: Int = Process.myUserHandle().identifier,
    targetPackageName: String = ref.nodePackage,
  ) : this(input) {
    this.nodeComponent = ref.nodeComponent
    this.nodeName = ref.nodeName
    this.creatorNodeId = creatorNodeId
    this.taskServiceIntent = taskServiceIntent
    this.targetUserId = targetUserId
    this.targetPackageName = targetPackageName
  }

  init {
    bundle.putString(EXTRA_TYPE, PendingContract.Task.TYPE)
  }

  override fun equals(other: Any?) =
    other is PendingTaskContract && bundle.contentDeepEquals(other.bundle)

  override fun hashCode() = bundle.generateHash()

  override fun toString() = "PendingContract.Task(${identity()})"

  override fun clone(): PendingTaskContract = PendingTaskContract(PersistableBundle(bundle))

  override var taskServiceIntent: PersistableIntent
    get() = checkNotNull(bundle.getPersistable(EXTRA_SERVICE_INTENT, PersistableIntent))
    set(value) {
      bundle.putPersistable(EXTRA_SERVICE_INTENT, value)
    }

  override var launchId: Long
    get() = bundle.getLong(EXTRA_LAUNCH_ID, UNKNOWN_NODE_ID)
    set(value) {
      bundle.putLong(EXTRA_LAUNCH_ID, value)
    }

  override var creatorNodeId: NodeId
    get() = bundle.getLong(EXTRA_CREATOR_ID, UNKNOWN_NODE_ID)
    set(value) {
      bundle.putLong(EXTRA_CREATOR_ID, value)
    }

  override var sourceNodeId: NodeId
    get() = bundle.getLong(EXTRA_SOURCE_ID, UNKNOWN_NODE_ID)
    set(value) {
      bundle.putLong(EXTRA_SOURCE_ID, value)
    }

  override val nodeId: NodeId
    get() = bundle.nodeId

  override var nodeComponent: String
    get() = checkNotNull(bundle.getString(EXTRA_COMPONENT))
    set(value) {
      bundle.putString(EXTRA_COMPONENT, value)
    }

  override var nodeName: String
    get() = checkNotNull(bundle.getString(EXTRA_NODE))
    set(value) {
      bundle.putString(EXTRA_NODE, value)
    }

  /** An internal space to store arbitrary onboarding metadata. */
  var extras: PersistableBundle?
    get() = bundle.getPersistableBundle(KEY_EXTRAS)
    set(value) {
      bundle.putPersistableBundle(KEY_EXTRAS, value)
    }

  /** An internal space to store arbitrary onboarding metadata. */
  override var targetUserId: Int
    get() = bundle.getInt(EXTRA_TARGET_USER_ID, Process.myUserHandle().identifier)
    set(value) {
      bundle.putInt(EXTRA_TARGET_USER_ID, value)
    }

  /** An internal space to store arbitrary onboarding metadata. */
  var targetPackageName: String
    get() = bundle.getString(EXTRA_TARGET_PACKAGE_NAME) ?: nodePackage
    set(value) {
      bundle.putString(EXTRA_TARGET_PACKAGE_NAME, value)
    }

  override fun getTargetUserId(input: Unit): Int = targetUserId

  override fun getTargetComponent(input: Unit): String = nodeComponent

  /** Put arbitrary onboarding metadata into [extras]. */
  fun putExtras(action: PersistableBundle.() -> Unit) {
    val current = extras ?: PersistableBundle()
    current.action()
    extras = current
  }

  override fun toPersistableBundle(): PersistableBundle = PersistableBundle(bundle)

  override val inputEncoder: Encoder<Unit, PersistableBundle>
    get() = Encoder { toPersistableBundle() }

  override val resultEncoder: Encoder<PersistableBundle, PersistableBundle>
    get() = Encoder { it }

  override val resultDecoder: Encoder<PersistableBundle, PersistableBundle>
    get() = Encoder { it }

  override fun <T> resolve(handler: ResultHandler<PersistableBundle, T>): PendingContract.Task<T> {

    return Resolved(raw = this, handler = handler)
  }

  /**
   * A wrapper over a [raw] [PendingTaskContract] delegating most functionality to it while
   * injecting [handler] to convert the result to a typesafe [R] type.
   *
   * @property raw the base [PendingTaskContract] to delegate to for source data.
   * @property handler the result handler to inject for typesafe result conversions.
   * @param R result type.
   */
  private class Resolved<R>(
    private val raw: PendingTaskContract,
    private val handler: ResultHandler<PersistableBundle, R>,
  ) :
    PendingContract.Task<R>,
    Parcelable by raw,
    PendingContractMetadata by raw,
    BackgroundLaunchable<Unit> by raw,
    ResultHandler<PersistableBundle, R> by handler,
    StableEquality by raw {

    override fun clone(): PendingContract.Task<R> = Resolved(raw = raw.clone(), handler = handler)

    override fun toString() = raw.toString()

    override fun toPersistableBundle(): PersistableBundle = raw.toPersistableBundle()

    override val creatorNodeId: NodeId
      get() = raw.creatorNodeId

    companion object CREATOR : Parcelable.Creator<PendingTaskContract> by PendingTaskContract
  }

  companion object CREATOR : Parcelable.Creator<PendingTaskContract> {
    private const val KEY_EXTRAS = "$NAMESPACE.task.EXTRAS"

    override fun createFromParcel(parcel: Parcel): PendingTaskContract =
      PendingTaskContract(PersistableBundle.CREATOR.createFromParcel(parcel))

    override fun newArray(size: Int): Array<PendingTaskContract?> = arrayOfNulls(size)
  }
}
