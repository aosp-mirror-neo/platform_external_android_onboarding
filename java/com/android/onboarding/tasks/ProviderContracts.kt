package com.android.onboarding.tasks

import android.content.Intent
import android.os.PersistableBundle
import com.android.onboarding.compatibility.compatGetBoolean
import com.android.onboarding.compatibility.compatPutBoolean
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.DEFAULT_TIMEOUT_IN_SECONDS
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.contracts.annotations.POnboardingNode
import com.android.onboarding.contracts.annotations.asNodeRef
import com.android.onboarding.control.annotations.CrossProcessSynchronous
import com.android.onboarding.control.annotations.HasTaskAlternative
import com.android.onboarding.control.annotations.PHasTaskAlternative
import com.android.onboarding.encoding.Encoder
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.generateHash
import com.android.onboarding.nodes.refersTo
import com.android.onboarding.pending.BackgroundLaunchable.Companion.prepareBindingIntent
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.tasks.precalculation.annotations.PPrecalculation
import com.android.onboarding.tasks.precalculation.annotations.PUsesPrecalculatedResult
import com.android.onboarding.tasks.precalculation.annotations.Precalculation
import com.android.onboarding.tasks.precalculation.annotations.UsesPrecalculatedResult
import com.android.onboarding.utils.KnownStableEquality
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.forceToString
import com.android.onboarding.utils.persistable.NullSafePersistableBundleExtensions.getIntOrThrow
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableBundle
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableIntent
import com.android.onboarding.utils.persistable.PersistableMapDeserializer
import com.android.onboarding.utils.persistable.PersistableMapSerializer
import com.android.onboarding.utils.persistable.PersistableUnit
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import java.time.Duration
import kotlin.reflect.KClass

/** A contract providing onboarding interactions across components via IPC. */
@OptIn(InternalOnboardingApi::class)
sealed class ProviderContract<I, O>
private constructor(final override val nodeComponent: String, final override val nodeName: String) :
  BaseOnboardingTaskContract<I, O>(), StableEquality {
  /**
   * @param component the [ComponentRef] of the target component providing the data.
   * @param type [ProviderContract] type used for routing. Usually this will simply be the class
   *   name, but the only requirement is that it is constant and backwards compatible.
   */
  constructor(component: ComponentRef, type: String) : this(component.nodeComponent, type)

  final override val timeout: Duration = Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECONDS)

  final override fun canAcceptInterrupt(reason: InterruptReason): Boolean = true

  final override fun toString(): String = "ProviderContract(${identity()}"

  final override fun equals(other: Any?) = other is ProviderContract<*, *> && this refersTo other

  final override fun hashCode() = generateHash()

  final override val taskServiceIntent: PersistableIntent by lazy { prepareBindingIntent(this) }

  /** Clean sensitive data from [input] for logging. */
  abstract fun cleanInput(input: I): String

  /**
   * A builder interface for [ProviderContract] linked to a specific [ComponentRef].
   * *
   */
  fun interface Builder<I, O> {
    fun build(component: ComponentRef): ProviderContract<I, O>
  }
}

/**
 * A [ProviderContract] for getting diagnostic information about the
 * [OnboardingTaskManagerServiceBinder].
 *
 * @param component a [ComponentRef] of the target component providing the data.
 */
class DiagnosisProviderContract(component: ComponentRef) :
  ProviderContract<DiagnosisProviderContract.Input, DiagnosisProviderContract.Output>(
    component = component,
    type = "DiagnosisProviderContract",
  ) {
  override val inputEncoder = Encoder(Input::toPersistableBundle)
  override val inputDecoder = Input
  override val resultEncoder = Encoder(Output::toPersistableBundle)
  override val resultDecoder = Output

  override fun cleanInput(input: Input): String = input.toString()

  /**
   * Input arguments to [DiagnosisProviderContract].
   *
   * @property delayFor if set, delay execution for specified duration.
   */
  data class Input(val delayFor: @KnownStableEquality Duration? = null) :
    Persistable, StableEquality {
    override fun toPersistableBundle() = PersistableBundle {
      putString(KEY_DELAY_FOR, delayFor?.toString())
    }

    companion object : PersistableDeserializer<Input> {
      private const val KEY_DELAY_FOR = "delayFor"

      override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
        Input(delayFor = persistableBundle.getString(KEY_DELAY_FOR)?.let(Duration::parse))
    }
  }

  /**
   * Outputs container for [DiagnosisProviderContract].
   *
   * @property ownerIdentityHash hash code of the owner (usually
   *   [OnboardingTaskManagerServiceBinder]) instance identity as implemented by the jvm.
   */
  data class Output(val ownerIdentityHash: Int) : Persistable, StableEquality {
    override fun toPersistableBundle() = PersistableBundle {
      putInt(KEY_OWNER_IDENTITY_HASH, ownerIdentityHash)
    }

    companion object : PersistableDeserializer<Output> {
      private const val KEY_OWNER_IDENTITY_HASH = "ownerIdentityHash"

      override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
        Output(ownerIdentityHash = persistableBundle.getIntOrThrow(KEY_OWNER_IDENTITY_HASH))
    }
  }

  companion object : Builder<Input, Output> by Builder(::DiagnosisProviderContract)
}

/**
 * A [ProviderContract] for getting a list of all [OnboardingNodes][OnboardingNode] target component
 * exposes for other provider interactions along with other annotation metadata each node contains.
 *
 * @param component a [ComponentRef] of the target component providing the data.
 */
class ContractMetadataProviderContract(component: ComponentRef) :
  ProviderContract<Unit, Map<NodeRef.Stable, ContractMetadata>>(
    component = component,
    type = "ContractMetadataProvider",
  ) {
  override val inputEncoder = Encoder(PersistableUnit.Unit::toPersistableBundle)
  override val inputDecoder = PersistableUnit.Unit
  override val resultEncoder = Encoder(PersistableMapSerializer.Companion::toPersistableBundle)
  override val resultDecoder =
    Encoder(PersistableMapDeserializer(NodeRef, ContractMetadata)::fromPersistableBundle)

  override fun cleanInput(input: Unit): String = nodeComponent

  companion object :
    Builder<Unit, Map<NodeRef.Stable, ContractMetadata>> by Builder(
      ::ContractMetadataProviderContract
    )
}

/**
 * All extracted annotation metadata for a given [NodeRef].
 *
 * @property onboardingNode The [OnboardingNode] annotations data.
 * @property usesPrecalculatedResult The [UsesPrecalculatedResult] annotation data.
 * @property precalculation The [Precalculation] annotation data.
 * @property hasTaskAlternative The [HasTaskAlternative] annotation data.
 * @property isCrossProcessSynchronous The presence of the [CrossProcessSynchronous] annotation.
 */

data class ContractMetadata
private constructor(
  val isTask: Boolean,
  val isActivity: Boolean,
  val onboardingNode: POnboardingNode,
  val workerId: Int,
  val usesPrecalculatedResult: PUsesPrecalculatedResult?,
  val precalculation: PPrecalculation?,
  val hasTaskAlternative: PHasTaskAlternative?,
  val isCrossProcessSynchronous: Boolean,
) : Persistable, NodeRef by onboardingNode.asNodeRef() {
  override fun toPersistableBundle() = PersistableBundle {
    putPersistable(KEY_ONBOARDING_NODE, onboardingNode)
    putInt(KEY_WORKER_ID, workerId)
    putPersistable(KEY_USES_PRECALCULATION_RESULT, usesPrecalculatedResult)
    putPersistable(KEY_PRECALCULATION, precalculation)
    putPersistable(KEY_HAS_TASK_ALTERNATIVE, hasTaskAlternative)
    compatPutBoolean(KEY_IS_TASK, isTask)
    compatPutBoolean(KEY_IS_ACTIVITY, isActivity)
    compatPutBoolean(KEY_IS_CROSS_PROCESS_SYNCHRONOUS, isCrossProcessSynchronous)
  }

  companion object : PersistableDeserializer<ContractMetadata> {
    private const val KEY_ONBOARDING_NODE = "ONBOARDING_NODE"
    private const val KEY_WORKER_ID = "WORKER_ID"
    private const val KEY_USES_PRECALCULATION_RESULT = "USES_PRECALCULATION_RESULT"
    private const val KEY_PRECALCULATION = "PRECALCULATION"
    private const val KEY_HAS_TASK_ALTERNATIVE = "HAS_TASK_ALTERNATIVE"
    private const val KEY_IS_TASK = "IS_TASK"
    private const val KEY_IS_ACTIVITY = "IS_ACTIVITY"
    private const val KEY_IS_CROSS_PROCESS_SYNCHRONOUS = "IS_CROSS_PROCESS_SYNCHRONOUS"

    /** Extract [ContractMetadata] from a given [OnboardingActivityApiContract] class. */
    @JvmName("fromActivity")
    @JvmStatic
    operator fun invoke(klass: KClass<out OnboardingActivityApiContract<*, *>>) =
      ContractMetadata(
        isTask = false,
        isActivity = true,
        onboardingNode =
          klass.findAnnotationsOfType<OnboardingNode>().single().let(::POnboardingNode),
        workerId = OnboardingNode.extractWorkerIdFromClass(klass.java),
        usesPrecalculatedResult =
          klass
            .findAnnotationsOfType<UsesPrecalculatedResult>()
            .singleOrNull()
            ?.let(::PUsesPrecalculatedResult),
        precalculation =
          klass.findAnnotationsOfType<Precalculation>().singleOrNull()?.let(::PPrecalculation),
        hasTaskAlternative =
          klass
            .findAnnotationsOfType<HasTaskAlternative>()
            .singleOrNull()
            ?.let(::PHasTaskAlternative),
        isCrossProcessSynchronous = false,
      )

    /** Extract [ContractMetadata] from a given [OnboardingActivityApiContract]. */
    @JvmName("fromActivity")
    @JvmStatic
    operator fun invoke(activity: OnboardingActivityApiContract<*, *>) = invoke(activity::class)

    /** Extract [ContractMetadata] from a given [OnboardingActivityApiContract] class. */
    @JvmName("fromActivity")
    @JvmStatic
    operator fun invoke(activity: Class<out OnboardingActivityApiContract<*, *>>) =
      invoke(activity.kotlin)

    /** Extract [ContractMetadata] from a given [OnboardingTaskContract]. */
    @JvmName("fromTask")
    @JvmStatic
    operator fun invoke(klass: KClass<out OnboardingTaskContract<*, *>>) =
      ContractMetadata(
        isTask = true,
        isActivity = false,
        onboardingNode =
          klass.findAnnotationsOfType<OnboardingNode>().single().let(::POnboardingNode),
        workerId = OnboardingNode.extractWorkerIdFromClass(klass.java),
        usesPrecalculatedResult =
          klass
            .findAnnotationsOfType<UsesPrecalculatedResult>()
            .singleOrNull()
            ?.let(::PUsesPrecalculatedResult),
        precalculation =
          klass.findAnnotationsOfType<Precalculation>().singleOrNull()?.let(::PPrecalculation),
        hasTaskAlternative =
          klass
            .findAnnotationsOfType<HasTaskAlternative>()
            .singleOrNull()
            ?.let(::PHasTaskAlternative),
        isCrossProcessSynchronous =
          (klass.findAnnotationsOfType<CrossProcessSynchronous>().isNotEmpty()),
      )

    /** Extract [ContractMetadata] from a given [OnboardingTaskContract] class. */
    @JvmName("fromTask")
    @JvmStatic
    operator fun invoke(task: Class<out OnboardingTaskContract<*, *>>) = invoke(task.kotlin)

    /** Extract [ContractMetadata] from a given [OnboardingTaskContract]. */
    @JvmName("fromTask")
    @JvmStatic
    operator fun invoke(task: OnboardingTaskContract<*, *>) = invoke(task::class)

    private inline fun <reified T : Annotation> KClass<*>.findAnnotationsOfType() =
      java.declaredAnnotations.filterIsInstance<T>()

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      ContractMetadata(
        isTask = persistableBundle.compatGetBoolean(KEY_IS_TASK),
        isActivity = persistableBundle.compatGetBoolean(KEY_IS_ACTIVITY),
        onboardingNode =
          requireNotNull(persistableBundle.getPersistable(KEY_ONBOARDING_NODE, OnboardingNode)),
        workerId = persistableBundle.getInt(KEY_WORKER_ID, OnboardingNode.UNSPECIFIED_WORKER),
        usesPrecalculatedResult =
          persistableBundle.getPersistable(KEY_USES_PRECALCULATION_RESULT, UsesPrecalculatedResult),
        precalculation = persistableBundle.getPersistable(KEY_PRECALCULATION, PPrecalculation),
        hasTaskAlternative =
          persistableBundle.getPersistable(KEY_HAS_TASK_ALTERNATIVE, HasTaskAlternative),
        isCrossProcessSynchronous =
          persistableBundle.compatGetBoolean(KEY_IS_CROSS_PROCESS_SYNCHRONOUS),
      )
  }
}

/**
 * A [ProviderContract] for getting a [PendingContract] of a given node launch via IPC.
 *
 * @param component a [ComponentRef] of the target component providing the data.
 */
class PendingContractProviderContract(component: ComponentRef) :
  ProviderContract<PendingContractProviderContract.Request, PendingContract.Erased<*>>(
    component = component,
    type = "PendingContractProvider",
  ) {
  override val inputEncoder = Encoder(Persistable::toPersistableBundle)
  override val inputDecoder = Request

  override val resultEncoder = Encoder(Persistable::toPersistableBundle)
  override val resultDecoder = PendingContract.Erased

  override fun cleanInput(input: Request): String =
    "${input.node.identity()}(hash=${generateHash(input)}, input=${input.input.forceToString()})"

  /**
   * A request for [PendingContract] for a given [node] launch via IPC.
   *
   * @property node the node to prepare the launch for.
   * @property input raw input to be passed to the contract for [PendingContract] preparation.
   */
  data class Request(val node: NodeRef, val input: PersistableBundle) :
    Persistable, NodeRef by node {
    /**
     * @param node the node to prepare the launch for.
     * @param input raw input to be passed to the contract for [PendingContract] preparation.
     */
    constructor(node: NodeRef, input: PersistableIntent) : this(node, input.toPersistableBundle())

    /**
     * @param node the node to prepare the launch for.
     * @param input raw input to be passed to the contract for [PendingContract] preparation. This
     *   will be converted to [PersistableBundle] via [PersistableIntent].
     */
    constructor(node: NodeRef, input: Intent) : this(node, PersistableIntent(input))

    override fun toPersistableBundle() =
      NodeRef.toPersistableBundle(node).apply { putPersistableBundle(KEY_INPUT, input) }

    companion object : PersistableDeserializer<Request> {
      private const val KEY_INPUT = "INPUT"

      override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
        Request(
          node = NodeRef.fromPersistableBundle(persistableBundle),
          input = requireNotNull(persistableBundle.getPersistableBundle(KEY_INPUT)),
        )
    }
  }

  companion object :
    Builder<Request, PendingContract.Erased<*>> by Builder(::PendingContractProviderContract)
}
