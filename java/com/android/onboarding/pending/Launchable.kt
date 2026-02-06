package com.android.onboarding.pending

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import com.android.onboarding.contracts.NodeResult
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.EXTRA_ONBOARDING_NODE_ID
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.OnboardingEvent
import com.android.onboarding.nodes.OnboardingGraphLog
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.nodes.activityNodeId
import com.android.onboarding.nodes.nodeId
import com.android.onboarding.utils.currentUserIdentifier
import com.android.onboarding.utils.logging.OnboardingLog as Log
import java.util.UUID

/** Onboarding entities that can be launched. */
interface Launchable<I> {
  /** Provides a [ActivityLauncher] to use during launches of this entity. */
  @InternalOnboardingApi fun launcher(input: I): ActivityLauncher<I>

  /** Returns the target user id for the launch, given the [input]. */
  fun getTargetUserId(input: I): Int = currentUserIdentifier
}

/**
 * Onboarding activities that can be launched for result.
 *
 * In general this interface should not be implemented outside of the Onboarding library.
 */
interface LaunchableActivityForResult<I, O> : Launchable<I> {
  @InternalOnboardingApi override fun launcher(input: I): ActivityLauncherForResult<I, O>

  /**
   * Build an [ActivityResultContract] wrapper that delegates to this [LaunchableActivityForResult].
   */
  @Suppress(
    // This API is used by onboarding to fulfil contract launches.
    "UseContractsNotStartActivity"
  )
  @InternalOnboardingApi
  fun toActivityResultContract(): ActivityResultContract<I, O> =
    object : ActivityResultContract<I, O>() {
      private lateinit var launcher: ActivityLauncherForResult<I, O>

      override fun createIntent(context: Context, input: I): Intent {
        launcher = launcher(input)
        return launcher.createIntent(context, input)
      }

      override fun parseResult(resultCode: Int, intent: Intent?): O =
        launcher.parseResult(resultCode, intent)

      override fun getSynchronousResult(context: Context, input: I): SynchronousResult<O>? {
        launcher = launcher(input)
        return launcher.getSynchronousResult(context, input)
      }
    }
}

/** A launcher for onboarding entities that can be launched directly. */
@InternalOnboardingApi
abstract class ActivityLauncher<I> : NodeRef {
  /**
   * Optional event hook for intent preparations just after an outgoing intent, [nodeId] and
   * [outgoingId] are prepared.
   *
   * @param expectsResult Does this launch expect a result
   * @param nodeId the [NodeId] of the source node that prepared this intent.
   * @param outgoingId the [NodeId] assigned to the node to be launched.
   */
  open fun onPrepareIntent(expectsResult: Boolean, nodeId: NodeId, outgoingId: NodeId) = Unit

  /** Override the [input] that gets logged as an argument in onboarding events. */
  open fun provideEventArgument(input: I): Any? = input

  /** Creates an [Intent] for this entity containing the given argument. */
  abstract fun provideIntent(context: Context, input: I): Intent

  /** Create an Intent with the intention of launching the contract without expecting a result. */
  fun createIntentDirectly(context: Context, input: I): Intent {
    // Injection point when we are passing control out of the current activity
    // without expecting a result
    val outgoingId = newOutgoingId()
    val nodeId = context.activityNodeId()
    val intent =
      provideIntent(context, input).apply { putExtra(EXTRA_ONBOARDING_NODE_ID, outgoingId) }

    onPrepareIntent(expectsResult = false, nodeId, outgoingId)

    OnboardingGraphLog.log(
      OnboardingEvent.ActivityNodeExecutedDirectly(
        sourceNodeId = nodeId,
        nodeId = outgoingId,
        nodeRef = this,
        argument = provideEventArgument(input),
      )
    )

    return intent
  }

  companion object {
    /**
     * Create a new ID to be used for the node started by this launchable.
     *
     * This is only used when starting a launchable - it is not used when extracting arguments
     * during the execution of a contract. In that case, the ID is extracted from the activity
     * intent.
     */
    fun newOutgoingId(): Long = UUID.randomUUID().leastSignificantBits
  }
}

/** A launcher for onboarding activities that can be launched for result. */
@InternalOnboardingApi
abstract class ActivityLauncherForResult<I, O>(private val logTag: String) : ActivityLauncher<I>() {
  constructor(base: ActivityLauncherForResult<*, *>) : this(base.logTag)

  open var forResultOutGoingId: NodeId = UNKNOWN_NODE_ID

  /**
   * Fetches the result without starting the activity.
   *
   * This can be optionally implemented, and should return null if a result cannot be fetched and
   * the activity should be started.
   */
  open fun provideSynchronousResult(
    context: Context,
    args: I,
  ): ActivityResultContract.SynchronousResult<O>? = null

  /** Extracts the result from the low-level representation [ActivityResult]. */
  abstract fun provideResult(result: ActivityResult): O

  /** Override the [result] that gets logged as a result in onboarding events. */
  open fun provideEventResult(result: O): Any? = result

  /** @see ActivityResultContract.createIntent */
  fun createIntent(context: Context, input: I): Intent {
    // Injection point when we are passing control out of the current activity
    val intent = provideIntent(context, input)
    val nodeId = context.activityNodeId()

    // We should assume forResultOutGoingId is already set because, in
    // activityResultRegistry.onLaunch, getSynchronousResult is always called first. Then
    // createIntent may be called afterwards.
    // We should expect a outgoingId created in getSynchronousResult and store it in
    // forResultOutGoingId.
    // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/ComponentActivity.kt
    if (forResultOutGoingId != UNKNOWN_NODE_ID) {
      intent.putExtra(EXTRA_ONBOARDING_NODE_ID, forResultOutGoingId)
    } else {
      // getSynchronousResult is not called. This may be not called from
      // activityResultRegistry.onLaunch. We will use the outgoing which was just created in
      // performCreateIntent.
      forResultOutGoingId = intent.getLongExtra(EXTRA_ONBOARDING_NODE_ID, UNKNOWN_NODE_ID)
      Log.v(logTag, "COMPLIANCE: getSynchronousResult was not called when creating intent.")
    }
    onPrepareIntent(expectsResult = true, nodeId, forResultOutGoingId)

    OnboardingGraphLog.log(
      OnboardingEvent.ActivityNodeExecutedForResult(
        sourceNodeId = nodeId,
        nodeId = forResultOutGoingId,
        nodeRef = this,
        argument = provideEventArgument(input),
      )
    )
    forResultOutGoingId = UNKNOWN_NODE_ID
    return intent
  }

  /** @see ActivityResultContract.parseResult */
  fun parseResult(resultCode: Int, intent: Intent?): O {
    // Injection point when control has returned to the current activity

    val id = intent?.nodeId ?: UNKNOWN_NODE_ID

    val result = provideResult(ActivityResult(resultCode, intent))

    OnboardingGraphLog.log(
      OnboardingEvent.ActivityNodeResultReceived(
        nodeId = id,
        nodeRef = this,
        result = provideEventResult(result),
      )
    )

    if (result is NodeResult.Failure) {
      OnboardingGraphLog.log(OnboardingEvent.ActivityNodeFail(id, result.toString()))
    }

    return result
  }

  /** @see ActivityResultContract.getSynchronousResult */
  fun getSynchronousResult(
    context: Context,
    input: I,
  ): ActivityResultContract.SynchronousResult<O>? {
    // Injection point when making a synchronous call
    val thisNodeId = context.activityNodeId()

    forResultOutGoingId = newOutgoingId()

    OnboardingGraphLog.log(
      OnboardingEvent.ActivityNodeStartExecuteSynchronously(
        sourceNodeId = thisNodeId,
        nodeId = forResultOutGoingId,
        nodeRef = this,
        argument = provideEventArgument(input),
      )
    )

    val result = provideSynchronousResult(context, input)
    if (result != null) {
      // Injection point when the synchronous result was used and the activity was skipped

      OnboardingGraphLog.log(
        OnboardingEvent.ActivityNodeExecutedSynchronously(
          nodeId = forResultOutGoingId,
          nodeRef = this,
          result = provideEventResult(result.value),
        )
      )
    }

    return result
  }

  /**
   * Build an [ActivityResultContract] wrapper that delegates to this [ActivityLauncherForResult].
   */
  @Suppress(
    // This API is used by onboarding to fulfil contract launches.
    "UseContractsNotStartActivity"
  )
  fun toActivityResultContract(): ActivityResultContract<I, O> =
    object : ActivityResultContract<I, O>() {
      override fun createIntent(context: Context, input: I): Intent =
        this@ActivityLauncherForResult.createIntent(context, input)

      override fun parseResult(resultCode: Int, intent: Intent?): O =
        this@ActivityLauncherForResult.parseResult(resultCode, intent)

      override fun getSynchronousResult(context: Context, input: I): SynchronousResult<O>? =
        this@ActivityLauncherForResult.getSynchronousResult(context, input)
    }

  /**
   * Maps this launcher to a launcher that accepts input type [II], transforms it back to type [I]
   * via [transform] and delegates all actions to this original launcher.
   */
  fun <II> mapInput(
    mapEventArgument: (II) -> Any? = { it },
    transform: (II) -> I,
  ): ActivityLauncherForResult<II, O> =
    object : ActivityLauncherForResult<II, O>(logTag) {
      override fun provideResult(result: ActivityResult): O =
        this@ActivityLauncherForResult.provideResult(result)

      override val nodeComponent: String = this@ActivityLauncherForResult.nodeComponent
      override val nodeName: String = this@ActivityLauncherForResult.nodeName

      override fun provideEventArgument(input: II): Any? = mapEventArgument(input)

      override fun provideIntent(context: Context, input: II): Intent =
        this@ActivityLauncherForResult.provideIntent(context, transform(input))
    }
}
