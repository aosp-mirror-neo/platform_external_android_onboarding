package com.android.onboarding.contracts

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.android.onboarding.contracts.annotations.ForegroundOnboardingNode
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.encoding.Encoder
import com.android.onboarding.encoding.ResultHandler
import com.android.onboarding.nodes.EXTRA_ONBOARDING_NODE_ID
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.OnboardingEvent
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeArgumentExtracted
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeExtractArgument
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeFail
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeFailedValidation
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeSetResult
import com.android.onboarding.nodes.OnboardingEvent.ActivityNodeValidating
import com.android.onboarding.nodes.OnboardingEvent.PendingContractPreparedForActivity
import com.android.onboarding.nodes.OnboardingGraphLog
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.nodes.activityNodeId
import com.android.onboarding.nodes.nodeId
import com.android.onboarding.pending.ActivityLauncherForResult
import com.android.onboarding.pending.Launchable
import com.android.onboarding.pending.LaunchableActivityForResult
import com.android.onboarding.pending.PendingActivityContract
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.getPersistableExtra
import com.google.common.util.concurrent.ListenableFuture
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.guava.future

/**
 * A Contract used for launching an activity as part of the Onboarding flow.
 *
 * It is required that all Activity starts in the Android Onboarding flow go via contracts. This is
 * to allow for better central tracking of onboarding sessions.
 *
 * Note that as a [NodeRef], this class represents the owner of the contract, not the package
 * executing the contract. For example, it is possible to have a contract owned by APP_1, but have
 * that contract produce an intent which launches an activity in APP_2.
 */
@OptIn(InternalOnboardingApi::class)
@Suppress(
  // This is the alternative that the lint suggests using.
  "UseContractsNotStartActivity"
)
abstract class OnboardingActivityApiContract<I, O> :
  ActivityResultContract<I, O>(),
  LaunchableActivityForResult<I, O>,
  NodeRef,
  ResultHandler<ActivityResult, O> {
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

  final override val nodeComponent: String by lazy(metadata::component)

  final override val nodeName by lazy(metadata::name)

  final override fun toActivityResultContract() = this

  final override val resultDecoder: Encoder<ActivityResult, O> = Encoder { performParseResult(it) }

  final override val resultEncoder: Encoder<O, ActivityResult> = Encoder { performSetResult(it) }

  // Set to a non-null value when parsing arguments and a [ResultRedirectionConfig.EXTRA_KEY] key
  // is present
  private var resultRedirectionConfig: ResultRedirectionConfig? = null

  @InternalOnboardingApi
  private val launcher =
    object : ActivityLauncherForResult<I, O>(TAG) {
      override val nodeComponent: String
        get() = this@OnboardingActivityApiContract.nodeComponent

      override val nodeName: String
        get() = this@OnboardingActivityApiContract.nodeName

      override fun provideResult(result: ActivityResult): O = performParseResult(result)

      override fun provideSynchronousResult(context: Context, args: I): SynchronousResult<O>? =
        performGetSynchronousResult(context, args)

      override fun provideIntent(context: Context, input: I): Intent =
        performCreateIntent(context, input)

      override fun onPrepareIntent(expectsResult: Boolean, nodeId: NodeId, outgoingId: NodeId) {
        super.onPrepareIntent(expectsResult, nodeId, outgoingId)
        if (!expectsResult) {
          // Track for resume event for activity node id.
          waitingForResumeActivity[nodeId] = outgoingId
        }
      }
    }

  override fun launcher(input: I): ActivityLauncherForResult<I, O> = launcher

  /**
   * Prepares a launch of this contract with supplied [input] without actually launching. Returned
   * [PendingContract] can safely be passed around between processes and invoked later.
   */
  fun prepare(context: Context, input: I): PendingContract.Activity<O> {
    val intent = performCreateIntent(context, input)
    val launch =
      PendingActivityContract(intent)
        .apply {
          nodeComponent = this@OnboardingActivityApiContract.nodeComponent
          nodeName = this@OnboardingActivityApiContract.nodeName
          creatorNodeId = context.activityNodeId()
          targetUserId = getTargetUserId(input)
        }
        .resolve(this)
    OnboardingGraphLog.log(
      PendingContractPreparedForActivity(
        nodeRef = this,
        nodeId = launch.creatorNodeId,
        argument = input,
      )
    )
    return launch
  }

  final override fun createIntent(context: Context, input: I): Intent =
    launcher(input).createIntent(context, input)

  final override fun parseResult(resultCode: Int, intent: Intent?): O =
    launcher.parseResult(resultCode, intent)

  final override fun getSynchronousResult(context: Context, input: I): SynchronousResult<O>? =
    launcher(input).getSynchronousResult(context, input)

  /**
   * Creates an [Intent] for this contract containing the given argument.
   *
   * This should be symmetric with [performExtractArgument].
   */
  protected abstract fun performCreateIntent(context: Context, arg: I): Intent

  /**
   * Extracts the argument from the given [Intent].
   *
   * This should be symmetric with [performCreateIntent].
   */
  protected abstract fun performExtractArgument(intent: Intent): I

  /**
   * Convert the given result into the low-level representation [ActivityResult].
   *
   * This should be symmetric with [performParseResult].
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  abstract fun performSetResult(result: O): ActivityResult

  /** Internal-only API for directly fetching the [ActivityResult] for this contract. */
  @InternalOnboardingApi fun executePerformSetResult(result: O) = performSetResult(result)

  /**
   * Extracts the result from the low-level representation [ActivityResult].
   *
   * This should be symmetric with [performSetResult].
   */
  abstract fun performParseResult(result: ActivityResult): O

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
    OnboardingGraphLog.log(
      ActivityNodeExtractArgument(
        nodeId = intent.nodeId,
        nodeRef = this,
        intent = intentToIntentData(intent),
      )
    )


    resultRedirectionConfig =
      intent.getPersistableExtra(ResultRedirectionConfig.EXTRA_KEY, ResultRedirectionConfig)

    val argument = performExtractArgument(intent)

    OnboardingGraphLog.log(
      ActivityNodeArgumentExtracted(nodeId = intent.nodeId, nodeRef = this, argument = argument)
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
  @VisibleForTesting
  internal fun setResult(activity: Activity, result: O) =
    setResult(activity.nodeId, activity::setResult, result)

  /** Sets a result for this contract. */
  @VisibleForTesting
  internal fun setResult(nodeId: NodeId, setResult: (Int, Intent?) -> Unit, result: O) {
    // Injection point when we are returning a result from the current activity

    OnboardingGraphLog.log(ActivityNodeSetResult(nodeId = nodeId, nodeRef = this, result = result))
    if (result is NodeResult.Failure) {
      OnboardingGraphLog.log(ActivityNodeFail(nodeId, result.toString()))
    }

    val activityResult = performSetResult(result)

    val intent = activityResult.data ?: Intent()
    intent.putExtra(EXTRA_ONBOARDING_NODE_ID, nodeId)

    setResult(activityResult.resultCode, intent)
  }

  /**
   * Object returned by [prepareResult] which contains data required to return a result.
   *
   * Multiple [PreparedResultReturner] can be stored at a time but only one which includes the
   * intended result should have [commit] called.
   */
  class PreparedResultReturner<in O, in A>(
    private val result: O,
    private val contract: OnboardingActivityApiContract<*, O>,
    private val getNodeId: A.() -> NodeId,
    private val setResult: A.(Int, Intent?) -> Unit,
    private val finish: A.() -> Unit,
    private val registerForActivityLaunch: A.(c: Launchable<Unit>) -> ActivityLauncher<Unit>,
    private val resultRedirectionConfig: ResultRedirectionConfig?,
  ) {

    /**
     * Commit the prepared result.
     *
     * By default this will finish the activity.
     */
    suspend fun commit(activity: A) {
      commitSync(activity)
    }

    /** See [commit]. */
    fun commitAsync(executor: Executor, activity: A): ListenableFuture<Void?> =
      CoroutineScope(executor.asCoroutineDispatcher()).future {
        commit(activity)
        null // We can't return Unit to a method expecting Void
      }

    /**
     * Commit the prepared result synchronously.
     *
     * Executing this synchronously significantly limits the amount of optimisations which can be
     * performed. [commit] or [commitAsync] should be preferred where the UX allows for it.
     */
    fun commitSync(activity: A) {
      contract.setResult(
        nodeId = activity.getNodeId(),
        setResult = { resultCode, data -> launchNext(activity, resultCode, data) },
        result = result,
      )
    }

    private fun launchNext(activity: A, resultCode: Int, data: Intent?) {
      val nextActivity = resultRedirectionConfig?.nextActivity
      if (nextActivity != null) {
        val preparedContract = nextActivity.onResult(ActivityResult(resultCode, data))
        activity.registerForActivityLaunch(preparedContract).launch()
      } else {
        activity.setResult(resultCode, data)
      }

      activity.finish()
    }
  }

  /**
   * Stores [result] ready for returning as the result of this contract.
   *
   * This returns a [PreparedResultReturner] which must then have the
   * [PreparedResultReturner.commit], [PreparedResultReturner.commitSync], or
   * [PreparedResultReturner.commitAsync] methods called to pass the result and end the contract
   * execution.
   *
   * It is recommended that callers prepare results as early as possible - even if the result is not
   * yet confirmed. This allows for optimisations
   */
  fun prepareResult(result: O): PreparedResultReturner<O, Activity> =
    prepareResult(
      getNodeId = Activity::nodeId,
      setResult = Activity::setResult,
      finish = Activity::finish,
      result = result,
      registerForActivityLaunch = Activity::registerForActivityLaunch,
    )

  @InternalOnboardingApi
  fun <A> prepareResult(
    getNodeId: A.() -> NodeId,
    setResult: A.(Int, Intent?) -> Unit,
    finish: A.() -> Unit,
    result: O,
    registerForActivityLaunch: A.(c: Launchable<Unit>) -> ActivityLauncher<Unit>,
    resultRedirectionConfig: ResultRedirectionConfig? = this.resultRedirectionConfig,
  ): PreparedResultReturner<O, A> =
    PreparedResultReturner(
      result = result,
      contract = this,
      getNodeId = getNodeId,
      setResult = setResult,
      finish = finish,
      registerForActivityLaunch = registerForActivityLaunch,
      resultRedirectionConfig = resultRedirectionConfig,
    )

  class OnboardingLifecycleObserver<I, O>(
    private val componentName: () -> ComponentName,
    private val intent: Intent,
    private val isFinishing: () -> Boolean,
    private val contract: OnboardingActivityApiContract<I, O>,
  ) : LifecycleEventObserver {
    constructor(
      activity: Activity,
      contract: OnboardingActivityApiContract<I, O>,
      intent: Intent = activity.intent,
    ) : this(activity::getComponentName, intent, activity::isFinishing, contract)

    private var isFinishLogged = false

    private fun maybeLogFinish() {
      if (!isFinishLogged && isFinishing() == true) {
        isFinishLogged = true
        OnboardingGraphLog.log(
          OnboardingEvent.ActivityNodeFinished(nodeId = intent.nodeId, nodeRef = contract)
        )
      }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
      when (event) {
        Lifecycle.Event.ON_STOP,
        Lifecycle.Event.ON_PAUSE -> maybeLogFinish()

        Lifecycle.Event.ON_DESTROY -> {
          maybeLogFinish()
          waitingForResumeActivity.remove(intent.nodeId)
        }

        Lifecycle.Event.ON_RESUME -> {
          val nodeId = intent.nodeId
          if (nodeId == UNKNOWN_NODE_ID) {
            Log.w(TAG, "$componentName does not contain node id.")
          }
          OnboardingGraphLog.log(
            OnboardingEvent.ActivityNodeResumed(nodeId = nodeId, nodeRef = contract)
          )
          val waitingForResume = nodeId != UNKNOWN_NODE_ID && nodeId in waitingForResumeActivity
          if (waitingForResume) {
            val sourceNodeId = waitingForResumeActivity[nodeId] ?: UNKNOWN_NODE_ID
            OnboardingGraphLog.log(
              OnboardingEvent.ActivityNodeResumedAfterLaunch(
                sourceNodeId = sourceNodeId,
                nodeId = nodeId,
                nodeRef = contract,
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
  fun attach(activity: ComponentActivity, intent: Intent = activity.intent): AttachedResult =
    attach(
      componentName = activity::getComponentName,
      isFinishing = activity::isFinishing,
      lifecycleOwner = activity,
      intent = intent,
      onBackPressedDispatcher = activity.onBackPressedDispatcher,
    )

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
  ): AttachedResult =
    attach(
      componentName = activity::getComponentName,
      isFinishing = activity::isFinishing,
      lifecycleOwner =
        lifecycle?.let {
          object : LifecycleOwner {
            override val lifecycle: Lifecycle = lifecycle
          }
        },
      intent = intent,
      onBackPressedDispatcher = null,
    )

  @InternalOnboardingApi
  fun attach(
    componentName: () -> ComponentName,
    isFinishing: () -> Boolean,
    lifecycleOwner: LifecycleOwner?,
    intent: Intent,
    onBackPressedDispatcher: OnBackPressedDispatcher?,
  ): AttachedResult {
    if (
      lifecycleOwner != null &&
        onBackPressedDispatcher != null &&
        metadata.foreground.backButtonBehavior ==
          ForegroundOnboardingNode.BackButtonBehavior.DISABLED
    ) {
      Log.d(TAG, "Automatically disabling back button due to backButtonBehavior metadata")
      onBackPressedDispatcher.addCallback(
        lifecycleOwner,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            // Do nothing
            Log.w(
              TAG,
              "Back button pressed but automatically disabled by OnboardingActivityApiContract",
            )
          }
        },
      )
    }
    val validated = validateInternal(intent)
    lifecycleOwner
      ?.lifecycle
      ?.addObserver(OnboardingLifecycleObserver(componentName, intent, isFinishing, this))
    return AttachedResult(validated)
  }

  /**
   * Validate that the intent can be parsed into an argument.
   *
   * When parsing fails, the failure will be recorded so that it can be fixed.
   *
   * @param activity the current activity context
   * @param intent the [Intent] to validate (defaults to the activity's current intent)
   * @return `true` if the intent is valid, `false` otherwise
   * @deprecated use [attach].
   */
  @CanIgnoreReturnValue
  @Deprecated("Use attach instead", ReplaceWith("attach(activity, intent)"))
  fun validate(activity: ComponentActivity, intent: Intent = activity.intent): Boolean {
    @Suppress("DEPRECATION")
    return validate(activity, activity.lifecycle, intent)
  }

  /**
   * Validate that the intent can be parsed into an argument.
   *
   * When parsing fails, the failure will be recorded so that it can be fixed.
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
    val validated = validateInternal(intent)
    lifecycle?.addObserver(
      OnboardingLifecycleObserver(activity = activity, contract = this, intent = intent)
    )
    return validated
  }

  private fun validateInternal(intent: Intent): Boolean {
    OnboardingGraphLog.log(
      ActivityNodeValidating(
        nodeId = intent.nodeId,
        nodeRef = this,
        intent = intentToIntentData(intent),
      )
    )

    return runCatching { extractArgument(intent) }
      .onFailure {
        OnboardingGraphLog.log(
          ActivityNodeFailedValidation(
            nodeId = intent.nodeId,
            nodeRef = this,
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

    private const val TAG = "OnboardingApiContract"

    /** nodeId to outgoingId */
    private val waitingForResumeActivity: MutableMap<NodeId, NodeId> = mutableMapOf()
  }
}

/** Returns [true] if the activity was launched using onboarding contract, [false] otherwise. */
fun Activity.isLaunchedByOnboardingContract(): Boolean {
  return intent.hasExtra(EXTRA_ONBOARDING_NODE_ID)
}

fun OnboardingActivityApiContract<Unit, *>.createIntent(context: Context) =
  createIntent(context, Unit)

/** See [OnboardingActivityApiContract.prepare]. */
fun OnboardingActivityApiContract<Unit, *>.prepare(context: Context): PendingContract.Activity<*> =
  prepare(context, Unit)
