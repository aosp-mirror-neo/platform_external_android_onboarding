package com.android.onboarding.pending

import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import androidx.activity.result.ActivityResult
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.encoding.ResultHandler
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.OnboardingEvent
import com.android.onboarding.nodes.OnboardingGraphLog
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.nodes.activityNodeId
import com.android.onboarding.nodes.nodeId
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_COMPONENT
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_CREATOR_ID
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_LAUNCH_ID
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_NODE
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_OUTGOING_RESULT_ID
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_SOURCE_ID
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_TARGET_USER_ID
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_TYPE
import com.android.onboarding.pending.PendingContractMetadata.Companion.NAMESPACE
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.currentUserIdentifier
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableIntent

@InternalOnboardingApi
data class PendingActivityContract(private val intent: PersistableIntent) :
  PendingContract.ErasedActivity, Parcelable by intent, Persistable, PendingContractMetadata {
  constructor(intent: Intent) : this(PersistableIntent(intent))

  constructor(
    intent: PersistableIntent,
    input: PersistableBundle,
    ref: NodeRef,
    creatorNodeId: NodeId,
    targetUserId: Int,
  ) : this(intent) {
    this.nodeComponent = ref.nodeComponent
    this.nodeName = ref.nodeName
    this.creatorNodeId = creatorNodeId
    this.intent.extras.putAll(input)
    this.targetUserId = targetUserId
  }

  override fun toIntent(): Intent = intent.toIntent()

  override fun toPersistableIntent(): PersistableIntent = PersistableIntent(intent)

  override fun toPersistableBundle() =
    intent.toPersistableBundle().apply { putString(EXTRA_TYPE, PendingContract.Activity.TYPE) }

  override fun clone() = PendingActivityContract(PersistableIntent(intent.toIntent()))

  override fun erase(): PendingContract.ErasedActivity = this

  override fun toString() =
    "PendingContract.Activity(" +
      "contract=${identity()}," +
      "action=${intent.action}," +
      "package=${intent.`package`}," +
      "targetUserId=${targetUserId}" +
      ")"

  /**
   * The ID assigned to this prepared contract launch. It is expected to survive process boundaries
   * as this launch is being passed around.
   */
  override var launchId: Long
    get() = intent.extras.getLong(EXTRA_LAUNCH_ID, UNKNOWN_NODE_ID)
    set(value) {
      intent.extras.putLong(EXTRA_LAUNCH_ID, value)
    }

  /** A [NodeId] of the node that originally prepared this launch. */
  override var creatorNodeId: NodeId
    get() = intent.extras.getLong(EXTRA_CREATOR_ID, UNKNOWN_NODE_ID)
    set(value) {
      intent.extras.putLong(EXTRA_CREATOR_ID, value)
    }

  /** A [NodeId] of the node which executed this launch. */
  override var sourceNodeId: NodeId
    get() = intent.extras.getLong(EXTRA_SOURCE_ID, UNKNOWN_NODE_ID)
    set(value) {
      intent.extras.putLong(EXTRA_SOURCE_ID, value)
    }

  /** A [NodeId] of the node this launch is prepared to invoke. */
  override val nodeId: NodeId
    get() = intent.extras.nodeId

  override var nodeComponent: String
    get() = checkNotNull(intent.extras.getString(EXTRA_COMPONENT))
    set(value) {
      intent.extras.putString(EXTRA_COMPONENT, value)
    }

  override var nodeName: String
    get() = checkNotNull(intent.extras.getString(EXTRA_NODE))
    set(value) {
      intent.extras.putString(EXTRA_NODE, value)
    }

  override var targetUserId: Int
    get() = intent.extras.getInt(EXTRA_TARGET_USER_ID, currentUserIdentifier)
    set(value) {
      intent.extras.putInt(EXTRA_TARGET_USER_ID, value)
    }

  @InternalOnboardingApi
  override fun launcher(input: Unit): ActivityLauncherForResult<Unit, ActivityResult> =
    object :
      ActivityLauncherForResult<Unit, ActivityResult>("PendingActivityApiContract"),
      NodeRef by this@PendingActivityContract {

      override var forResultOutGoingId: NodeId
        get() = extras?.getLong(EXTRA_OUTGOING_RESULT_ID, UNKNOWN_NODE_ID) ?: UNKNOWN_NODE_ID
        set(value) {
          putExtras { putLong(EXTRA_OUTGOING_RESULT_ID, value) }
        }

      override fun onPrepareIntent(expectsResult: Boolean, nodeId: NodeId, outgoingId: NodeId) {
        super.onPrepareIntent(expectsResult, nodeId, outgoingId)
        OnboardingGraphLog.log(
          OnboardingEvent.PendingContractLaunchedForActivity(
            nodeId = outgoingId,
            nodeRef = this,
            creatorNodeId = creatorNodeId,
            sourceNodeId = nodeId,
            launchId = launchId,
          )
        )
      }

      override fun provideResult(result: ActivityResult): ActivityResult = result

      override fun provideIntent(context: Context, input: Unit): Intent {
        val clone = this@PendingActivityContract.clone()
        clone.sourceNodeId = context.activityNodeId()
        clone.extras?.remove(EXTRA_OUTGOING_RESULT_ID)
        return clone.intent.toIntent()
      }
    }

  /** An internal space to store arbitrary onboarding metadata. */
  var extras: PersistableBundle?
    get() = intent.extras.getPersistableBundle(KEY_EXTRAS)
    set(value) {
      if (value == null) {
        intent.extras.remove(KEY_EXTRAS)
      } else {
        intent.extras.putPersistableBundle(KEY_EXTRAS, value)
      }
    }

  /** Put arbitrary onboarding metadata into [extras]. */
  fun putExtras(action: PersistableBundle.() -> Unit) {
    val current = extras ?: PersistableBundle()
    current.action()
    extras = current
  }

  override fun <T> resolve(handler: ResultHandler<ActivityResult, T>): PendingContract.Activity<T> {

    return Resolved(raw = this, handler = handler)
  }

  /**
   * A wrapper over a [raw] [PendingActivityContract] delegating most functionality to it while
   * injecting [handler] to convert the result to a typesafe [R] type.
   *
   * @property raw the base [PendingActivityContract] to delegate to for source data.
   * @property handler the result handler to inject for typesafe result conversions.
   * @param R result type.
   */
  private class Resolved<R>(
    private val raw: PendingActivityContract,
    private val handler: ResultHandler<ActivityResult, R>,
  ) :
    PendingContract.Activity<R>,
    Parcelable by raw,
    PendingContractMetadata by raw,
    NodeRef by raw,
    StableEquality by raw {
    override fun clone(): PendingContract.Activity<R> =
      Resolved(raw = raw.clone(), handler = handler)

    override fun toPersistableBundle(): PersistableBundle = raw.toPersistableBundle()

    override fun toIntent(): Intent = raw.toIntent()

    override fun toPersistableIntent(): PersistableIntent = raw.toPersistableIntent()

    override fun toString() = raw.toString()

    override val creatorNodeId: NodeId
      get() = raw.creatorNodeId

    @InternalOnboardingApi
    override fun launcher(input: Unit): ActivityLauncherForResult<Unit, R> {
      val rawLauncher = raw.launcher(input)
      return object : ActivityLauncherForResult<Unit, R>(rawLauncher), NodeRef by rawLauncher {
        override fun provideIntent(context: Context, input: Unit): Intent =
          rawLauncher.provideIntent(context, input)

        override fun onPrepareIntent(direct: Boolean, nodeId: NodeId, outgoingId: NodeId) =
          rawLauncher.onPrepareIntent(direct, nodeId, outgoingId)

        override fun provideEventArgument(input: Unit): Any? =
          rawLauncher.provideEventArgument(input)

        override fun provideResult(result: ActivityResult): R = handler.resultDecoder.encode(result)
      }
    }

    companion object CREATOR :
      Parcelable.Creator<PendingActivityContract> by PendingActivityContract
  }

  companion object CREATOR : Parcelable.Creator<PendingActivityContract> {
    /** Key used when storing extras. */
    const val KEY_EXTRAS = "$NAMESPACE.activity.EXTRAS"

    override fun createFromParcel(parcel: Parcel): PendingActivityContract =
      PendingActivityContract(Intent.CREATOR.createFromParcel(parcel))

    override fun newArray(size: Int): Array<PendingActivityContract?> = arrayOfNulls(size)
  }
}
