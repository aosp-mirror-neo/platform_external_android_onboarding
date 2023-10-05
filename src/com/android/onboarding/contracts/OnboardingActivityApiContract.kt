package com.android.onboarding.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.android.onboarding.contracts.ContractResult.Failure
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.AndroidOnboardingGraphLog
import com.android.onboarding.nodes.EXTRA_ONBOARDING_NODE_ID
import com.android.onboarding.nodes.OnboardingGraphLog
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeArgumentExtracted
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeExecutedDirectly
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeExecutedForResult
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeExecutedSynchronously
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeExtractArgument
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeFail
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeFailedValidation
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeResultReceived
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeSetResult
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeStartExecuteSynchronously
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeValidating
import java.util.UUID

/**
 * A Contract used for launching an activity as part of the Onboarding flow.
 *
 * <p>It is required that all Activity starts in the Android Onboarding flow go via contracts. This
 * is to allow for better central tracking of onboarding sessions.
 */
abstract class OnboardingActivityApiContract<I, O> : ActivityResultContract<I, O>() {

  /**
   * The ID to be used for the node started by this contract.
   *
   * <p>This is only used when starting a contract - it is not used when extracting arguments during
   * the execution of a contract. In that case, the ID is extracted from the activity intent.
   */
  private val outgoingId by lazy { UUID.randomUUID().leastSignificantBits }

  /**
   * Creates an {@link Intent} for this contract containing the given argument.
   *
   * <p>This should be symmetric with [performExtractArgument].
   */
  protected abstract fun performCreateIntent(context: Context, arg: I): Intent

  /**
   * Extracts the argument from the given [Intent].
   *
   * <p>This should be symmetric with [performCreateIntent].
   */
  protected abstract fun performExtractArgument(intent: Intent): I

  /**
   * Sets the given result.
   *
   * This should be symmetric with [performParseResult].
   */
  protected abstract fun performSetResult(result: O): ContractResult

  /**
   * Extracts the result from the given [ContractResult].
   *
   * This should be symmetric with [performSetResult].
   */
  protected abstract fun performParseResult(result: ContractResult): O

  /**
   * Fetches the result without starting the activity.
   *
   * <p>This can be optionally implemented, and should return null if a result cannot be fetched and
   * the activity should be started.
   */
  protected open fun performGetSynchronousResult(context: Context, args: I): SynchronousResult<O>? =
    null

  /** Extracts an argument passed into the current activity using the contract. */
  fun extractArgument(intent: Intent): I {
    // Injection point when we are receiving control in an activity
    AndroidOnboardingGraphLog.log(
      ActivityNodeExtractArgument(intent.nodeId, this.javaClass.name, intentToIntentData(intent))
    )

    val argument = performExtractArgument(intent)

    AndroidOnboardingGraphLog.log(
      ActivityNodeArgumentExtracted(intent.nodeId, this.javaClass.name, argument)
    )

    return argument
  }

  private fun intentToIntentData(intent: Intent): OnboardingGraphLog.OnboardingEvent.IntentData {
    val extras = buildMap<String, Any?> {
        intent.extras?.let { for (key in it.keySet()) put(key, it.get(key)) }
      }

    return OnboardingGraphLog.OnboardingEvent.IntentData(intent.action, extras)
  }

  /** Sets a result for this contract. */
  fun setResult(activity: Activity, result: O) {
    // Injection point when we are returning a result from the current activity

    AndroidOnboardingGraphLog.log(
      ActivityNodeSetResult(activity.nodeId, this.javaClass.name, result)
    )

    var contractResult = performSetResult(result)

    if (contractResult is Failure) {
      AndroidOnboardingGraphLog.log(
        ActivityNodeFail(activity.nodeId, this.javaClass.name, contractResult.reason)
      )
    }

    val intent = contractResult.intent ?: Intent()
    intent.putExtra(EXTRA_ONBOARDING_NODE_ID, activity.nodeId)

    activity.setResult(contractResult.resultCode, intent)
  }

  /**
   * Validate that the intent can be parsed into an argument.
   *
   * <p>When parsing fails, the failure will be recorded so that it can be fixed.
   */
  fun validate(activity: Activity, intent: Intent = activity.intent) {
    try {
      AndroidOnboardingGraphLog.log(
        ActivityNodeValidating(activity.nodeId, this.javaClass.name, intentToIntentData(intent))
      )

      val unused = extractArgument(intent)
    } catch (e: Exception) {
      AndroidOnboardingGraphLog.log(
        ActivityNodeFailedValidation(
          activity.nodeId,
          this.javaClass.name,
          e,
          intentToIntentData(intent)
        )
      )
    }
  }

  private fun extractNodeId(context: Context) =
    if (context is Activity) {
      // This is true in all known cases - but we need to accept Context because that's the AndroidX
      // API surface
      context.nodeId
    } else {
      UNKNOWN_NODE
    }

  /** Create an Intent with the intention of launching the contract without expecting a result. */
  internal fun createIntentDirectly(context: Context, input: I): Intent {
    // Injection point when we are passing control out of the current activity without expecting a
    // result
    val intent = performCreateIntent(context, input)
    intent.putExtra(EXTRA_ONBOARDING_NODE_ID, outgoingId)

    AndroidOnboardingGraphLog.log(
      ActivityNodeExecutedDirectly(extractNodeId(context), outgoingId, this.javaClass.name, input)
    )

    return intent
  }

  final override fun createIntent(context: Context, input: I): Intent {
    // Injection point when we are passing control out of the current activity
    val intent = performCreateIntent(context, input)
    intent.putExtra(EXTRA_ONBOARDING_NODE_ID, outgoingId)

    AndroidOnboardingGraphLog.log(
      ActivityNodeExecutedForResult(extractNodeId(context), outgoingId, this.javaClass.name, input)
    )

    return intent
  }

  @OptIn(InternalOnboardingApi::class)
  final override fun parseResult(resultCode: Int, intent: Intent?): O {
    // Injection point when control has returned to the current activity

    val id = intent?.nodeId ?: UNKNOWN_NODE

    val result = performParseResult(UnknownContractResult(resultCode, intent))

    AndroidOnboardingGraphLog.log(ActivityNodeResultReceived(id, this.javaClass.name, result))

    if (result is Failure) {
      AndroidOnboardingGraphLog.log(ActivityNodeFail(id, this.javaClass.name, result.reason))
    }

    return result
  }

  final override fun getSynchronousResult(context: Context, input: I): SynchronousResult<O>? {
    // Injection point when making a synchronous call

    val thisNodeId = extractNodeId(context)

    AndroidOnboardingGraphLog.log(
      ActivityNodeStartExecuteSynchronously(thisNodeId, outgoingId, this.javaClass.name, input)
    )

    val result = performGetSynchronousResult(context, input)
    if (result != null) {
      // Injection point when the synchronous result was used and the activity was skipped

      AndroidOnboardingGraphLog.log(
        ActivityNodeExecutedSynchronously(outgoingId, this.javaClass.name, result.value)
      )
    }

    return result
  }

  companion object {
    const val UNKNOWN_NODE = -1L
  }
}

/** Equivalent to [OnboardingActivityApiContract] for contracts which do not return a result. */
abstract class VoidOnboardingActivityApiContract<I> : OnboardingActivityApiContract<I, Unit>() {
  final override fun performSetResult(result: Unit): ContractResult {
    // Does nothing - no result
    return Failure(0, reason = "Should never be called")
  }

  final override fun performParseResult(result: ContractResult) {
    // Does nothing - no result
  }
}

/** Equivalent to [OnboardingActivityApiContract] for contracts which do not take arguments. */
abstract class ArgumentFreeOnboardingActivityApiContract<O> :
  OnboardingActivityApiContract<Unit, O>() {
  final override fun performExtractArgument(intent: Intent) {
    // Does nothing - no argument
  }
}

/** Equivalent to [VoidOnboardingActivityApiContract] for contracts which do not take arguments. */
abstract class ArgumentFreeVoidOnboardingActivityApiContract :
  VoidOnboardingActivityApiContract<Unit>() {
  final override fun performExtractArgument(intent: Intent) {
    // Does nothing - no argument
  }
}

/** Returns [true] if the activity is launched using onboarding contract, [false] otherwise. */
fun Activity.isLaunchedByOnboardingContract(): Boolean {
  return this.intent.hasExtra(EXTRA_ONBOARDING_NODE_ID)
}
