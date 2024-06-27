package com.android.onboarding.contracts

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import com.android.onboarding.bedsteadonboarding.contractutils.ContractExecutionEligibilityChecker
import com.android.onboarding.bedsteadonboarding.contractutils.ContractUtils
import com.android.onboarding.nodes.AndroidOnboardingGraphLog
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.OnboardingEvent
import java.util.UUID

/** Onboarding entities that can be launched. */
interface Launchable<I> {
  /** Provides a [Launcher] to use during launches of this entity. */
  val launcher: Launcher<I>
}

/** Onboarding entities that can be launched for result. */
interface LaunchableForResult<I, O> : Launchable<I> {
  override val launcher: LauncherForResult<I, O>
}

/** A launcher for onboarding entities that can be launched directly. */
abstract class Launcher<I> : NodeRef {
  /**
   * Optional event hook for intent preparations just after an outgoing intent, [nodeId] and
   * [outgoingId] are prepared.
   */
  protected open fun onPrepareIntent(nodeId: NodeId, outgoingId: NodeId) {}

  /** Extract a [NodeId] from a given [context]. */
  protected abstract fun extractNodeId(context: Context): NodeId

  /** Creates an [Intent] for this entity containing the given argument. */
  protected abstract fun provideIntent(context: Context, input: I): Intent

  /** Create an Intent with the intention of launching the contract without expecting a result. */
  internal fun createIntentDirectly(context: Context, input: I): Intent {
    // Injection point when we are passing control out of the current activity
    // without expecting a result
    val outgoingId = newOutgoingId()
    val nodeId = extractNodeId(context)
    val intent =
      provideIntent(context, input).apply { putExtra(EXTRA_ONBOARDING_NODE_ID, outgoingId) }

    onPrepareIntent(nodeId, outgoingId)

    AndroidOnboardingGraphLog.log(
      OnboardingEvent.ActivityNodeExecutedDirectly(
        sourceNodeId = nodeId,
        nodeId = outgoingId,
        nodeComponent = nodeComponent,
        nodeName = nodeName,
        argument = input,
      )
    )

    ContractExecutionEligibilityChecker.terminateIfNodeIsTriggeredByTestAndIsNotAllowed(
      context = context,
      contractIdentifier =
        ContractUtils.getContractIdentifier(nodeComponent = nodeComponent, nodeName = nodeName),
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
    internal fun newOutgoingId(): Long = UUID.randomUUID().leastSignificantBits
  }
}

/** A launcher for onboarding entities that can be launched for result. */
abstract class LauncherForResult<I, O>(private val tag: String) : Launcher<I>() {
  protected open var forResultOutGoingId: NodeId = UNKNOWN_NODE_ID

  /**
   * Fetches the result without starting the activity.
   *
   * This can be optionally implemented, and should return null if a result cannot be fetched and
   * the activity should be started.
   */
  protected open fun provideSynchronousResult(
    context: Context,
    args: I,
  ): ActivityResultContract.SynchronousResult<O>? = null

  /** Extracts the result from the low-level representation [ActivityResult]. */
  protected abstract fun provideResult(result: ActivityResult): O

  /** @see ActivityResultContract.createIntent */
  internal fun createIntent(context: Context, input: I): Intent {
    // Injection point when we are passing control out of the current activity
    val intent = provideIntent(context, input)
    val nodeId = extractNodeId(context)

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
      Log.w(tag, "getSynchronousResult was not called when creating intent.")
    }

    AndroidOnboardingGraphLog.log(
      OnboardingEvent.ActivityNodeExecutedForResult(
        sourceNodeId = nodeId,
        nodeId = forResultOutGoingId,
        nodeComponent = nodeComponent,
        nodeName = nodeName,
        argument = input,
      )
    )
    forResultOutGoingId = UNKNOWN_NODE_ID
    return intent
  }

  /** @see ActivityResultContract.parseResult */
  internal fun parseResult(resultCode: Int, intent: Intent?): O {
    // Injection point when control has returned to the current activity

    val id = intent?.nodeId ?: UNKNOWN_NODE_ID

    val result = provideResult(ActivityResult(resultCode, intent))

    AndroidOnboardingGraphLog.log(
      OnboardingEvent.ActivityNodeResultReceived(
        nodeId = id,
        nodeComponent = nodeComponent,
        nodeName = nodeName,
        result = result,
      )
    )

    if (result is NodeResult.Failure) {
      AndroidOnboardingGraphLog.log(OnboardingEvent.ActivityNodeFail(id, result.toString()))
    }

    return result
  }

  /** @see ActivityResultContract.getSynchronousResult */
  internal fun getSynchronousResult(
    context: Context,
    input: I,
  ): ActivityResultContract.SynchronousResult<O>? {
    // Injection point when making a synchronous call

    val contractIdentifier = ContractUtils.getContractIdentifier(nodeComponent, nodeName)
    ContractUtils.getContractResultIfNodeIsFakedInTest(context, contractIdentifier)?.let { result ->
      Log.i(tag, "Contract result fetched for fake node $contractIdentifier is $result")
      return ActivityResultContract.SynchronousResult(provideResult(result.toActivityResult()))
    }

    val thisNodeId = extractNodeId(context)

    forResultOutGoingId = newOutgoingId()

    AndroidOnboardingGraphLog.log(
      OnboardingEvent.ActivityNodeStartExecuteSynchronously(
        sourceNodeId = thisNodeId,
        nodeId = forResultOutGoingId,
        nodeComponent = nodeComponent,
        nodeName = nodeName,
        argument = input,
      )
    )

    ContractExecutionEligibilityChecker.terminateIfNodeIsTriggeredByTestAndIsNotAllowed(
      context = context,
      contractIdentifier =
        ContractUtils.getContractIdentifier(nodeComponent = nodeComponent, nodeName = nodeName),
    )

    val result = provideSynchronousResult(context, input)
    if (result != null) {
      // Injection point when the synchronous result was used and the activity was skipped

      AndroidOnboardingGraphLog.log(
        OnboardingEvent.ActivityNodeExecutedSynchronously(
          nodeId = forResultOutGoingId,
          nodeComponent = nodeComponent,
          nodeName = nodeName,
          result = result.value,
        )
      )
    }

    return result
  }

  /** Build an [ActivityResultContract] wrapper that delegates to this [LauncherForResult]. */
  open fun toActivityResultContract(): ActivityResultContract<I, O> =
    object : ActivityResultContract<I, O>() {
      override fun createIntent(context: Context, input: I): Intent =
        this@LauncherForResult.createIntent(context, input)

      override fun parseResult(resultCode: Int, intent: Intent?): O =
        this@LauncherForResult.parseResult(resultCode, intent)

      override fun getSynchronousResult(context: Context, input: I): SynchronousResult<O>? =
        this@LauncherForResult.getSynchronousResult(context, input)
    }
}
