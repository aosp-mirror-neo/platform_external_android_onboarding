package com.android.onboarding.tasks

import android.os.PersistableBundle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.contracts.annotations.asNodeRef
import com.android.onboarding.encoding.Encoder
import com.android.onboarding.nodes.EXTRA_ONBOARDING_NODE_ID
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.OnboardingEvent
import com.android.onboarding.nodes.OnboardingGraphLog
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.pending.BackgroundLaunchable.Companion.prepareBindingIntent
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.pending.PendingTaskContract
import com.android.onboarding.tasks.OnboardingTaskToken.Companion.EXTRA_COMPONENT
import com.android.onboarding.tasks.OnboardingTaskToken.Companion.EXTRA_NODE
import com.android.onboarding.utils.generateHash
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableIntent
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Abstract class representing the contract for an onboarding task.
 *
 * This class defines the contract for an onboarding task, specifying how task arguments and results
 * should be handled. Implementing classes are expected to provide concrete implementations for
 * encoding and extracting task arguments/results.
 *
 * @param I The type representing the task arguments.
 * @param O The type representing the task result.
 */
@OptIn(InternalOnboardingApi::class)
abstract class OnboardingTaskContract<I, O> : BaseOnboardingTaskContract<I, O>() {
  /**
   * Extracted [OnboardingNode] metadata for this contract.
   *
   * Resolved lazily and throws an error if the annotation is not present.
   */
  @InternalOnboardingApi val metadata: OnboardingNode by lazy(this::class::onboardingNode)
  private val acceptsInterrupts: Array<InterruptReason> by lazy {

    try {
      metadata.background.acceptsInterrupts
    } catch (e: Exception) {
      Log.e(
        TAG,
        "Failed to extract `acceptsInterrupts` from metadata - assuming no accepted interrupts",
        e,
      )
      emptyArray()
    }
  }
  final override val nodeComponent: String by lazy(metadata::component)
  final override val nodeName: String by lazy(metadata::name)
  final override val taskServiceIntent: PersistableIntent by lazy { prepareBindingIntent(this) }

  override fun toString(): String = "OnboardingTaskContract(${identity()})"

  /**
   * Returns whether this contract will accept an interrupt for the given [reason].
   *
   * How an interrupt is handled is defined by the task implementation, in the [onInterrupt] method.
   *
   * The accepted interrupt reasons are defined in the [OnboardingNode] annotation. Specifically,
   * the [OnboardingNode.background.acceptsInterrupts] property.
   */
  final override fun canAcceptInterrupt(reason: InterruptReason): Boolean {
    return acceptsInterrupts.contains(reason)
  }

  final override val timeout: Duration by lazy {
    metadata.timeoutInSeconds.seconds.toJavaDuration()
  }

  /**
   * Prepares a launch of this contract with supplied [input] without actually launching. Returned
   * [PendingContract] can safely be passed around between processes and invoked later.
   */
  fun prepare(input: I): PendingContract.Task<O> {
    val bundle = performEncodeArgs(input)
    val launch =
      PendingTaskContract(
          input = bundle,
          ref = this,

          creatorNodeId = UNKNOWN_NODE_ID,
          taskServiceIntent = taskServiceIntent,
          targetUserId = getTargetUserId(input),
          targetPackageName = getTargetComponent(input),
        )
        .resolve(this)
    OnboardingGraphLog.log(
      OnboardingEvent.PendingContractPreparedForTask(
        nodeRef = this,
        nodeId = launch.creatorNodeId,
        argument = input,
      )
    )
    return launch
  }

  /**
   * Encodes task arguments additional contract information into a [PersistableBundle] object.
   *
   * @param args The task arguments to encode.
   * @return The encoded [PersistableBundle].
   */
  @Deprecated("Use inputEncoder", replaceWith = ReplaceWith("inputEncoder.encode(args)"))
  fun encodeArgs(args: I): PersistableBundle = inputEncoder.encode(args)

  override val inputEncoder: Encoder<I, PersistableBundle> = Encoder { args ->
    val bundle = performEncodeArgs(args)
    bundle.putString(EXTRA_COMPONENT, nodeComponent)
    bundle.putString(EXTRA_NODE, nodeName)
    bundle.putLong(EXTRA_ONBOARDING_NODE_ID, bundle.generateHash().toLong())
    return@Encoder bundle
  }

  /**
   * Encodes task arguments into a [PersistableBundle] object.
   *
   * @param taskArgs The task arguments to encode.
   * @return The encoded [PersistableBundle].
   */
  protected abstract fun performEncodeArgs(taskArgs: I): PersistableBundle

  override val inputDecoder: Encoder<PersistableBundle, I> = Encoder { bundle ->
    val args = performExtractArgs(bundle)
    return@Encoder args
  }

  /**
   * Extracts task arguments from a [PersistableBundle] and returns them as an [I].
   *
   * @param bundle The [PersistableBundle] containing the encoded task arguments.
   * @return The extracted task arguments.
   */
  @Deprecated("Use inputDecoder", replaceWith = ReplaceWith("inputDecoder.encode(bundle)"))
  fun extractArgs(bundle: PersistableBundle): I = inputDecoder.encode(bundle)

  /**
   * Extracts task arguments from a [PersistableBundle] and returns them as an [I].
   *
   * @param bundle The [PersistableBundle] containing the encoded task arguments.
   * @return The extracted task arguments.
   */
  protected abstract fun performExtractArgs(bundle: PersistableBundle): I

  final override val resultEncoder: Encoder<O, PersistableBundle> = Encoder(::performEncodeResult)

  @Deprecated("Use resultEncoder", replaceWith = ReplaceWith("resultEncoder.encode(result)"))
  fun encodeResult(result: O): PersistableBundle = resultEncoder.encode(result)

  /**
   * Encodes a task result into a [PersistableBundle].
   *
   * @param taskResult The task result to encode.
   * @return The encoded [PersistableBundle].
   */
  protected abstract fun performEncodeResult(taskResult: O): PersistableBundle

  final override val resultDecoder: Encoder<PersistableBundle, O> = Encoder(::performExtractResult)

  @Deprecated("Use resultDecoder", replaceWith = ReplaceWith("resultDecoder.encode(result)"))
  fun parseResult(result: PersistableBundle): O = resultDecoder.encode(result)

  /**
   * Extracts task results from a [PersistableBundle] and returns them as a [Result].
   *
   * @param bundle The [PersistableBundle] containing the encoded task results.
   * @return The extracted task results.
   */
  protected abstract fun performExtractResult(bundle: PersistableBundle): O

  companion object {
    private const val TAG = "OnboardingTaskContract"
  }
}

/** Equivalent to [OnboardingTaskContract] for contracts which do not take arguments. */
abstract class ArgumentFreeOnboardingTaskContract<O> : OnboardingTaskContract<Unit, O>() {
  override fun validate(taskArgs: Unit?): Boolean = true

  override fun performEncodeArgs(taskArgs: Unit): PersistableBundle = PersistableBundle()

  override fun performExtractArgs(bundle: PersistableBundle) {
    // Do nothing as no arguments.
  }
}

/** Equivalent to [OnboardingTaskContract] for contracts which do not return result. */
abstract class VoidOnboardingTaskContract<I> : OnboardingTaskContract<I, Unit>() {
  override fun performEncodeResult(taskResult: Unit): PersistableBundle = PersistableBundle()

  override fun performExtractResult(bundle: PersistableBundle) {
    // Do nothing as no result.
  }
}

/**
 * Equivalent to [OnboardingTaskContract] for contracts which do not take arguments or return
 * results.
 */
abstract class ArgumentFreeVoidOnboardingTaskContract : ArgumentFreeOnboardingTaskContract<Unit>() {
  override fun performEncodeResult(taskResult: Unit): PersistableBundle = PersistableBundle()

  override fun performExtractResult(bundle: PersistableBundle) {
    // Do nothing as no result.
  }
}

/** See [OnboardingTaskContract.prepare]. */
fun <O> OnboardingTaskContract<Unit, O>.prepare(): PendingContract.Task<O> = prepare(Unit)

/**
 * An [OnboardingTaskContract] with default implementations for encoding and extracting input and
 * results that implement [Persistable] and have a [PersistableDeserializer].
 */

abstract class OnboardingTaskContractWithPersistables<I : Persistable, O : Persistable> :
  OnboardingTaskContract<I, O>() {
  /** A [PersistableDeserializer] for [I]. By convention, this should be [I]'s companion object. */
  abstract val argsExtractor: PersistableDeserializer<I>
  /** A [PersistableDeserializer] for [O]. By convention, this should be [O]'s companion object. */
  abstract val resultExtractor: PersistableDeserializer<O>

  override fun performEncodeArgs(taskArgs: I): PersistableBundle = taskArgs.toPersistableBundle()

  override fun performExtractArgs(bundle: PersistableBundle): I =
    argsExtractor.fromPersistableBundle(bundle)

  override fun performEncodeResult(taskResult: O): PersistableBundle =
    taskResult.toPersistableBundle()

  override fun performExtractResult(bundle: PersistableBundle) =
    resultExtractor.fromPersistableBundle(bundle)
}

/** Extracts [OnboardingNode] annotation from this class. */
@InternalOnboardingApi
fun KClass<out OnboardingTaskContract<*, *>>.onboardingNode(): OnboardingNode =
  java.declaredAnnotations.filterIsInstance<OnboardingNode>().singleOrNull()
    ?: error("$qualifiedName is missing OnboardingNode annotation")

/** Extracts the [NodeRef] this contract class is pointing to. */
@OptIn(InternalOnboardingApi::class)
fun KClass<out OnboardingTaskContract<*, *>>.asNodeRef(): NodeRef = onboardingNode().asNodeRef()
