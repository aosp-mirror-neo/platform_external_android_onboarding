package com.android.onboarding.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.nodes.AndroidOnboardingGraphLog
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.OnboardingEvent
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeArgumentExtracted
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeExtractArgument
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeFail
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeFailedValidation
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeSetResult
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeValidating
import com.google.errorprone.annotations.CanIgnoreReturnValue

/**
 * A Contract used for launching an activity as part of the Onboarding flow.
 *
 * <p>It is required that all Activity starts in the Android Onboarding flow go via contracts. This
 * is to allow for better central tracking of onboarding sessions.
 */
abstract class OnboardingActivityApiContract<I, O> :
  ActivityResultContract<I, O>(), LaunchableForResult<I, O>, NodeRef {
  /**
   * Extracted [OnboardingNode] metadata for this contract.
   *
   * Resolved lazily and throws an error if the annotation is not present.
   */
  @InternalOnboardingApi
  val metadata: OnboardingNode by lazy {
    this::class.java.getAnnotation(OnboardingNode::class.java)
      ?: error("${this::class.qualifiedName} is missing OnboardingNode annotation")
  }

  @OptIn(InternalOnboardingApi::class)
  final override val nodeComponent: String by lazy(metadata::component)

  @OptIn(InternalOnboardingApi::class) final override val nodeName by lazy(metadata::name)

  override val launcher =
    object : LauncherForResult<I, O>(TAG) {
      override val nodeComponent: String
        get() = this@OnboardingActivityApiContract.nodeComponent

      override val nodeName: String
        get() = this@OnboardingActivityApiContract.nodeName

      override fun provideResult(result: ActivityResult): O = performParseResult(result)

      override fun provideSynchronousResult(context: Context, args: I): SynchronousResult<O>? =
        performGetSynchronousResult(context, args)

      override fun extractNodeId(context: Context): NodeId =
        this@OnboardingActivityApiContract.extractNodeId(context)

      override fun provideIntent(context: Context, input: I): Intent =
        performCreateIntent(context, input)

      override fun toActivityResultContract() = this@OnboardingActivityApiContract

      override fun onPrepareIntent(nodeId: NodeId, outgoingId: NodeId) {
        // Track for resume event for activity node id.
        waitingForResumeActivity[nodeId] = outgoingId
      }
    }

  /*
   * This is true in all known cases - but we need to accept Context because that's the
   * AndroidX
   * API surface
   */
  private fun extractNodeId(context: Context): NodeId = context.activityNodeId()

  /** Creates an {@link Intent} for this contract containing the given argument. */
  final override fun createIntent(context: Context, input: I): Intent =
    launcher.createIntent(context, input)

  final override fun parseResult(resultCode: Int, intent: Intent?): O =
    launcher.parseResult(resultCode, intent)

  final override fun getSynchronousResult(context: Context, input: I): SynchronousResult<O>? =
    launcher.getSynchronousResult(context, input)

  /**
   * Creates an [Intent] for this contract containing the given argument.
   *
   * This should be symmetric with [performExtractArgument].
   */
  protected abstract fun performCreateIntent(context: Context, arg: I): Intent

  /**
   * Extracts the argument from the given [Intent].
   *
   * <p>This should be symmetric with [performCreateIntent].
   */
  protected abstract fun performExtractArgument(intent: Intent): I

  /**
   * Convert the given result into the low-level representation [ActivityResult].
   *
   * This should be symmetric with [performParseResult].
   */
  protected abstract fun performSetResult(result: O): ActivityResult

  /**
   * Extracts the result from the low-level representation [ActivityResult].
   *
   * This should be symmetric with [performSetResult].
   */
  protected abstract fun performParseResult(result: ActivityResult): O

  /**
   * Fetches the result without starting the activity.
   *
   * This can be optionally implemented, and should return null if a result cannot be fetched and
   * the activity should be started.
   */
  protected open fun performGetSynchronousResult(context: Context, args: I): SynchronousResult<O>? =
    null

  /** Extracts an argument passed into the current activity using the contract. */
  fun extractArgument(intent: Intent): I {
    // Injection point when we are receiving control in an activity
    AndroidOnboardingGraphLog.log(
      ActivityNodeExtractArgument(intent.nodeId, this.javaClass, intentToIntentData(intent))
    )

    val argument = performExtractArgument(intent)

    AndroidOnboardingGraphLog.log(
      ActivityNodeArgumentExtracted(intent.nodeId, this.javaClass, argument)
    )

    return argument
  }

  private fun intentToIntentData(intent: Intent): OnboardingEvent.IntentData {
    val extras =
      buildMap<String, Any?> {
        intent.extras?.let { for (key in it.keySet()) put(key, it.get(key)) }
      }

    return OnboardingEvent.IntentData(intent.action, extras)
  }

  /** Sets a result for this contract. */
  fun setResult(activity: Activity, result: O) {
    // Injection point when we are returning a result from the current activity

    AndroidOnboardingGraphLog.log(ActivityNodeSetResult(activity.nodeId, this.javaClass, result))
    if (result is NodeResult.Failure) {
      AndroidOnboardingGraphLog.log(ActivityNodeFail(activity.nodeId, result.toString()))
    }

    val activityResult = performSetResult(result)

    val intent = activityResult.data ?: Intent()
    intent.putExtra(EXTRA_ONBOARDING_NODE_ID, activity.nodeId)

    activity.setResult(activityResult.resultCode, intent)
  }

  class OnboardingLifecycleObserver<I, O>(
    private var activity: Activity?,
    private val contract: OnboardingActivityApiContract<I, O>,
  ) : LifecycleEventObserver {
    private var isFinishLogged = false

    private fun maybeLogFinish() {
      if (!isFinishLogged && activity?.isFinishing == true) {
        isFinishLogged = true
        AndroidOnboardingGraphLog.log(
          OnboardingEvent.ActivityNodeFinished(
            activity?.nodeId ?: UNKNOWN_NODE_ID,
            contract.javaClass,
          )
        )
      }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
      when (event) {
        Lifecycle.Event.ON_STOP,
        Lifecycle.Event.ON_PAUSE -> maybeLogFinish()
        Lifecycle.Event.ON_DESTROY -> {
          maybeLogFinish()
          activity?.nodeId?.let { waitingForResumeActivity.remove(it) }
          // Clear the activity reference to avoid memory leak.
          activity = null
        }
        Lifecycle.Event.ON_RESUME -> {
          val nodeId = activity?.nodeId ?: UNKNOWN_NODE_ID
          if (nodeId == UNKNOWN_NODE_ID) {
            Log.w(TAG, "${activity?.componentName} does not contain node id.")
          }
          AndroidOnboardingGraphLog.log(
            OnboardingEvent.ActivityNodeResumed(nodeId, contract.javaClass)
          )
          val waitingForResume =
            nodeId != UNKNOWN_NODE_ID && waitingForResumeActivity.contains(nodeId)
          if (waitingForResume) {
            val sourceNodeId = waitingForResumeActivity[nodeId] ?: UNKNOWN_NODE_ID
            AndroidOnboardingGraphLog.log(
              OnboardingEvent.ActivityNodeResumedAfterLaunch(
                sourceNodeId,
                nodeId,
                contract.javaClass,
              )
            )
            waitingForResumeActivity.remove(nodeId)
          }
        }
        Lifecycle.Event.ON_CREATE,
        Lifecycle.Event.ON_START,
        Lifecycle.Event.ON_ANY -> {}
      }
    }
  }

  /**
   * Attaches to the specified Activity in onCreate. This validates the intent can be parsed into an
   * argument.
   *
   * @param activity The Activity to attach to.
   * @param intent The Intent to use (defaults to the Activity's intent).
   * @return An AttachedResult object with information about the attachment, including the
   *   validation result.
   */
  @CanIgnoreReturnValue
  fun attach(activity: ComponentActivity, intent: Intent = activity.intent): AttachedResult {
    return attach(activity, activity.lifecycle, intent)
  }

  /**
   * Attaches to the specified Activity in onCreate. This validates the intent can be parsed into an
   * argument.
   *
   * A lifecycle should be provided. If the activity does not support lifecycle owner, it should
   * implement a LifeCycleOwner or migrate to use androidx ComponentActivity.
   *
   * @param activity The Activity to attach to.
   * @param lifecycle The Lifecycle of the activity.
   * @param intent The Intent to use (defaults to the Activity's intent).
   * @return An AttachedResult object with information about the attachment, including the
   *   validation result.
   */
  @CanIgnoreReturnValue
  fun attach(
    activity: Activity,
    lifecycle: Lifecycle?,
    intent: Intent = activity.intent,
  ): AttachedResult {
    val validated = validateInternal(activity, intent)
    lifecycle?.addObserver(OnboardingLifecycleObserver(activity, this))
    return AttachedResult(validated)
  }

  /**
   * Validate that the intent can be parsed into an argument.
   *
   * <p>When parsing fails, the failure will be recorded so that it can be fixed.
   *
   * @param activity the current activity context
   * @param intent the [Intent] to validate (defaults to the activity's current intent)
   * @return `true` if the intent is valid, `false` otherwise
   * @deprecated use [attach].
   */
  @CanIgnoreReturnValue
  @Deprecated("Use attach instead", ReplaceWith("attach(activity, intent)"))
  fun validate(activity: ComponentActivity, intent: Intent = activity.intent): Boolean {
    return validate(activity, activity.lifecycle, intent)
  }

  /**
   * Validate that the intent can be parsed into an argument.
   *
   * <p>When parsing fails, the failure will be recorded so that it can be fixed.
   *
   * @param activity the current activity context
   * @param lifecycle the lifecycle of the activity, used for logging purposes
   * @param intent the [Intent] to validate (defaults to the activity's current intent)
   * @return `true` if the intent is valid, `false` otherwise
   */
  @CanIgnoreReturnValue
  @Deprecated("Use attach instead", ReplaceWith("attach(activity, lifecycle, intent)"))
  fun validate(
    activity: Activity,
    lifecycle: Lifecycle?,
    intent: Intent = activity.intent,
  ): Boolean {
    val validated = validateInternal(activity, intent)
    lifecycle?.addObserver(OnboardingLifecycleObserver(activity, this))
    return validated
  }

  private fun validateInternal(activity: Activity, intent: Intent): Boolean {
    AndroidOnboardingGraphLog.log(
      ActivityNodeValidating(activity.nodeId, this.javaClass, intentToIntentData(intent))
    )
    return runCatching { extractArgument(intent) }
      .onFailure {
        AndroidOnboardingGraphLog.log(
          ActivityNodeFailedValidation(
            nodeId = activity.nodeId,
            nodeClass = this.javaClass,
            exception = it,
            intent = intentToIntentData(intent),
          )
        )
      }
      .map { true }
      .getOrDefault(false)
  }

  companion object {
    @Deprecated(
      message = "Moved",
      replaceWith =
        ReplaceWith(
          "UNKNOWN_NODE_ID",
          imports = ["com.android.onboarding.contracts.UNKNOWN_NODE_ID"],
        ),
    )
    const val UNKNOWN_NODE: NodeId = UNKNOWN_NODE_ID

    const val TAG = "OnboardingApiContract"

    // nodeId to outgoingId
    private val waitingForResumeActivity: MutableMap<NodeId, NodeId> = mutableMapOf()
  }
}

/** Equivalent to [OnboardingActivityApiContract] for contracts which do not return a result. */
abstract class VoidOnboardingActivityApiContract<I> : OnboardingActivityApiContract<I, Unit>() {
  final override fun performSetResult(result: Unit): ActivityResult {
    // Does nothing - no result
    return ActivityResult(resultCode = 0, data = null)
  }

  final override fun performParseResult(result: ActivityResult) {
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

  final override fun performCreateIntent(context: Context, arg: Unit) = performCreateIntent(context)

  abstract fun performCreateIntent(context: Context): Intent
}

/** Returns [true] if the activity is launched using onboarding contract, [false] otherwise. */
fun Activity.isLaunchedByOnboardingContract(): Boolean {
  return intent.hasExtra(EXTRA_ONBOARDING_NODE_ID)
}
