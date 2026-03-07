package com.android.onboarding.pending

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.os.PersistableBundle
import androidx.activity.result.ActivityResult
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.encoding.Encoder
import com.android.onboarding.encoding.ResultHandler
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.OnboardingEvent.Companion.ERASED_ARGUMENT
import com.android.onboarding.pending.PendingContractMetadata.Companion.EXTRA_TYPE
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableIntent


/**
 * A wrapper packaging a prepared contract launch with its arguments and necessary onboarding
 * metadata. Note that multiple invocations of this pending contract will use the same
 * [creatorNodeId] regardless which node eventually executes it.
 */

@OptIn(InternalOnboardingApi::class)
sealed interface PendingContract<R> : Parcelable, NodeRef, Persistable, StableEquality {
  /** A [NodeId] of the node that originally prepared this launch. */
  val creatorNodeId: NodeId

  /** Clone this pending contract for new launches. */
  fun clone(): PendingContract<R>

  /** Erase this [PendingContract] and make it serializable. */
  fun erase(): Erased<*>

  /** Convert this to a desired onboarding event argument representation. */
  fun toEventArgument(): Any? = ERASED_ARGUMENT

  companion object : PersistableDeserializer<Erased<*>> by Erased


  interface Activity<R> : PendingContract<R>, LaunchableActivityForResult<Unit, R> {
    override fun erase(): ErasedActivity = PendingActivityContract(toIntent())

    override fun clone(): Activity<R>

    /**
     * Creates an [Intent] representing this [PendingContract.Activity].
     *
     * Unlike [createIntent] this method has no side effects.
     */
    fun toIntent(): Intent

    /**
     * Creates a [PersistableIntent] representing this [PendingContract.Activity].
     *
     * Unlike [createIntent] this method has no side effects.
     */
    fun toPersistableIntent(): PersistableIntent

    /**
     * Creates an [Intent] representing this [PendingContract.Activity].
     *
     * Results in launch onboarding events being logged and as such should be invoked as close to
     * the launch site as possible.
     */
    fun createIntent(context: Context): Intent = launcher(Unit).createIntent(context, Unit)

    companion object :
      LaunchableActivityForResult<ErasedActivity, ActivityResult>,
      PersistableDeserializer<ErasedActivity> by ErasedActivity {
      internal const val TYPE = "ACTIVITY"

      @InternalOnboardingApi
      override fun launcher(
        input: ErasedActivity
      ): ActivityLauncherForResult<ErasedActivity, ActivityResult> =
        input.launcher(Unit).mapInput(
          mapEventArgument = ErasedActivity::toEventArgument
        ) { /* noop as we're converting to Unit */
        }
    }
  }


  interface Task<R> : PendingContract<R>, BackgroundLaunchableForResult<Unit, R>, StableEquality {
    /** Erase this [PendingContract] and make it serializable. */
    override fun erase(): ErasedTask = PendingTaskContract(toPersistableBundle())

    companion object : PersistableDeserializer<ErasedTask> by ErasedTask {
      internal const val TYPE = "TASK"
    }
  }

  /**
   * A [PendingContract] which has erased details of its result type into a [Raw] transfer layer
   * type. This makes it suitable for serialization and processing by components which do not know
   * or have access to the return type. The typesafe results of these pending contracts cannot be
   * accessed. Callers must first call [resolve] to convert into a regular typed [PendingContract],
   * for which the results can be accessed.
   *
   * @see ErasedActivity
   * @see ErasedTask
   */

  sealed interface Erased<Raw> : Persistable, Parcelable, NodeRef, StableEquality {
    /**
     * Resolve the future results of this [PendingContract] with the given [ResultHandler] to make
     * it typesafe.
     *
     * Create a [PendingContract] which uses the given [ResultHandler] to extract the typesafe
     * result. This is required before extracting a typesafe result. Note that this call is not
     * type-safe. Resolving with an invalid [ResultHandler] will result in an eventual error on
     * result access.
     *
     * @param handler the [ResultHandler] to resolve the result with.
     * @param T the new typesafe result type.
     */
    fun <T> resolve(handler: ResultHandler<Raw, T>): PendingContract<T>

    companion object : PersistableDeserializer<Erased<*>> {
      override fun fromPersistableBundle(persistableBundle: PersistableBundle): Erased<*> =
        when (val type = persistableBundle.getString(EXTRA_TYPE)) {
          Activity.TYPE -> Activity.fromPersistableBundle(persistableBundle)
          Task.TYPE -> Task.fromPersistableBundle(persistableBundle)
          else -> error("Type $type is unrecognised")
        }
    }
  }

  /**
   * A [PendingContract.Activity] which erases the results to a [ActivityResult] for serialization.
   *
   * @see Erased
   * @see Activity
   */

  sealed interface ErasedActivity : Activity<ActivityResult>, Erased<ActivityResult> {
    /**
     * Resolve the future results of this activity with the given [ResultHandler] to make it
     * typesafe. In most of the cases it will be an instance of
     * [OnboardingActivityApiContract][com.android.onboarding.contracts.OnboardingActivityApiContract]
     *
     * @param handler the [ResultHandler] to resolve the result with.
     * @param T the new typesafe result type.
     * @see Erased.resolve
     */
    override fun <T> resolve(handler: ResultHandler<ActivityResult, T>): Activity<T>

    /**
     * The user id of the user to launch the activity for. If not set, the activity will be launched
     * for the current user.
     */
    val targetUserId: Int

    companion object : PersistableDeserializer<ErasedActivity> {

      @OptIn(InternalOnboardingApi::class)
      override fun fromPersistableBundle(persistableBundle: PersistableBundle): ErasedActivity =
        PendingActivityContract(intent = PersistableIntent.fromPersistableBundle(persistableBundle))
    }
  }

  /**
   * A [PendingContract.Task] which erases the results to a [PersistableBundle] for serialization.
   *
   * @see Erased
   * @see Task
   */

  sealed interface ErasedTask : Task<PersistableBundle>, Erased<PersistableBundle> {
    /**
     * Resolve the future results of this task with the given [ResultHandler] to make it typesafe.
     * In most of the cases it will be an instance of
     * [OnboardingTaskContract][com.android.onboarding.tasks.OnboardingTaskContract]
     *
     * @param handler the [ResultHandler] to resolve the result with.
     * @param T the new typesafe result type.
     * @see Erased.resolve
     */
    override fun <T> resolve(handler: ResultHandler<PersistableBundle, T>): Task<T>

    /**
     * The user id of the user to launch the activity for. If not set, the activity will be launched
     * for the current user.
     */
    val targetUserId: Int

    companion object : PersistableDeserializer<ErasedTask> {
      @OptIn(InternalOnboardingApi::class)
      override fun fromPersistableBundle(persistableBundle: PersistableBundle): ErasedTask =
        PendingTaskContract(persistableBundle)
    }
  }
}

/**
 * A [ResultHandler] which can be used to [PendingContract.Erased.resolve] a contract with a Unit
 * result.
 */
object UnitResultHandler : ResultHandler<PersistableBundle, Unit> {
  override val resultDecoder: Encoder<PersistableBundle, Unit> = Encoder {}
  override val resultEncoder: Encoder<Unit, PersistableBundle> = Encoder { PersistableBundle() }
}

/** Internal metadata associated with [PendingContract]. */
internal interface PendingContractMetadata {
  /**
   * The ID assigned to this prepared contract launch. It is expected to survive process boundaries
   * as this launch is being passed around.
   */
  val launchId: Long

  /** A [NodeId] of the node which executed this launch. */
  val sourceNodeId: NodeId

  /** A [NodeId] of the node this launch is prepared to invoke. */
  val nodeId: NodeId

  companion object {
    const val NAMESPACE = "com.android.onboarding.pending"
    const val EXTRA_LAUNCH_ID = "$NAMESPACE.LAUNCH_ID"
    const val EXTRA_CREATOR_ID = "$NAMESPACE.CREATOR_ID"
    const val EXTRA_SOURCE_ID = "$NAMESPACE.SOURCE_ID"
    const val EXTRA_OUTGOING_RESULT_ID = "$NAMESPACE.OUTGOING_RESULT_ID"
    const val EXTRA_COMPONENT = "$NAMESPACE.COMPONENT"
    const val EXTRA_NODE = "$NAMESPACE.NODE"
    const val EXTRA_TYPE = "$NAMESPACE.TYPE"
    const val EXTRA_SERVICE_INTENT = "$NAMESPACE.SERVICE_INTENT"
    const val EXTRA_TARGET_USER_ID = "$NAMESPACE.TARGET_USER_ID"
    const val EXTRA_TARGET_PACKAGE_NAME = "$NAMESPACE.TARGET_PACKAGE_NAME"
    const val EXTRA_TIMEOUT_IN_SECONDS = "$NAMESPACE.TIMEOUT_IN_SECONDS"
  }
}
