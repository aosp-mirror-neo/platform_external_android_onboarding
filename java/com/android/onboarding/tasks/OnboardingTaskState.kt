package com.android.onboarding.tasks

import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.encoding.Encoder
import com.android.onboarding.encoding.WithResultDecoder
import com.android.onboarding.metadata.IString
import com.android.onboarding.metadata.NonSensitiveString
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.metadata.SensitiveString
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.UniqueNodeRef
import com.android.onboarding.nodes.nodeId
import com.android.onboarding.nodes.refersTo
import com.android.onboarding.tasks.OnboardingTaskState.Companion.EXTRA_LAST_METADATA
import com.android.onboarding.tasks.OnboardingTaskState.Companion.EXTRA_RESULT
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.contentDeepEquals
import com.android.onboarding.utils.datasplitter.getPendingDataIdentifier
import com.android.onboarding.utils.datasplitter.getPendingDataSizeInBytes
import com.android.onboarding.utils.datasplitter.getPendingDataTotalChunks
import com.android.onboarding.utils.datasplitter.isPendingBundle
import com.android.onboarding.utils.datasplitter.isTooLargeForTransaction
import com.android.onboarding.utils.datasplitter.removePendingBundleExtras
import com.android.onboarding.utils.datasplitter.splitForTransaction
import com.android.onboarding.utils.datasplitter.toPendingBundle
import com.android.onboarding.utils.forceToString
import com.android.onboarding.utils.generateHash
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import java.util.Objects

/**
 * Local-only variant of the [UnresolvedOnboardingTaskState] preserving reference to the associated
 * contract. Note that it needs to be erased via [toErased] call in order for it to be passable
 * across process boundaries.
 */
sealed class OnboardingTaskState<out O> : UnresolvedOnboardingTaskState<O> {
  protected abstract val parser: Encoder<PersistableBundle, O>
  protected abstract val nodeRef: NodeRef

  override val nodeComponent: String
    get() = nodeRef.nodeComponent

  override val nodeName: String
    get() = nodeRef.nodeName

  protected abstract val type: String

  @InternalOnboardingApi
  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().also { bundle ->
      bundle.nodeId = nodeId
      bundle.putString(OnboardingTaskToken.EXTRA_COMPONENT, nodeComponent)
      bundle.putString(OnboardingTaskToken.EXTRA_NODE, nodeName)
      bundle.putString(EXTRA_TYPE, type)
    }

  /** Represents the in-progress state of an onboarding task. */
  data class InProgress<out O>(
    override val parser: Encoder<PersistableBundle, O>,
    override val nodeRef: NodeRef,
    override val nodeId: NodeId,
  ) : OnboardingTaskState<O>() {
    internal constructor(
      parser: Encoder<PersistableBundle, O>,
      nodeRef: NodeRef,
      bundle: PersistableBundle,
    ) : this(parser = parser, nodeRef = nodeRef, nodeId = bundle.nodeId)

    constructor(
      token: OnboardingTaskToken<O>
    ) : this(parser = token.resultDecoder, nodeRef = token, nodeId = token.nodeId)

    override val type: String = TYPE_IN_PROGRESS

    override fun <OO> resolve(parser: WithResultDecoder<PersistableBundle, OO>): InProgress<OO> =
      resolve(parser.resultDecoder)

    override fun <OO> resolve(parser: Encoder<PersistableBundle, OO>): InProgress<OO> =
      InProgress(parser, nodeRef, nodeId)

    override val isCompleted = false

    override val lastMetadata = null

    override fun equals(other: Any?) = other is InProgress<*> && other refersTo this

    override fun hashCode() = Objects.hash(nodeComponent, nodeName, nodeId)

    override fun toString() = "OnboardingTaskState.InProgress(${identity()}"
  }

  sealed class Completed<out O>(
    private val resultBundle: PersistableBundle?,
    /** The last emitted metadata of the completed task. */
    override val lastMetadata: OnboardingMetadataStore?,
  ) : OnboardingTaskState<O>() {
    /** The result of the completed task. */
    @Suppress("UNCHECKED_CAST")
    val result: O by lazy { resultBundle?.let(parser::encode) ?: null as O }

    @InternalOnboardingApi
    override fun toPersistableBundle(): PersistableBundle =
      super.toPersistableBundle().apply {
        putPersistableBundle(EXTRA_RESULT, resultBundle)
        putPersistable(EXTRA_LAST_METADATA, lastMetadata)
      }

    abstract override fun <OO> resolve(
      parser: WithResultDecoder<PersistableBundle, OO>
    ): Completed<OO>

    abstract override fun <OO> resolve(parser: Encoder<PersistableBundle, OO>): Completed<OO>

    override val isCompleted = true
  }

  /** Represents the completed state of an onboarding task. */
  @OptIn(InternalOnboardingApi::class)
  data class Success<out O>
  @InternalOnboardingApi
  constructor(
    override val parser: Encoder<PersistableBundle, O>,
    override val nodeRef: NodeRef,
    override val nodeId: NodeId,
    private val resultBundle: PersistableBundle?,
    override val lastMetadata: OnboardingMetadataStore?,
  ) : Completed<O>(resultBundle, lastMetadata) {
    internal constructor(
      parser: Encoder<PersistableBundle, O>,
      nodeRef: NodeRef,
      bundle: PersistableBundle,
    ) : this(
      parser = parser,
      nodeRef = nodeRef,
      nodeId = bundle.nodeId,
      resultBundle = bundle.getPersistableBundle(EXTRA_RESULT),
      lastMetadata = bundle.getPersistable(EXTRA_LAST_METADATA, OnboardingMetadataStore),
    )

    constructor(
      token: OnboardingTaskToken<O>,
      result: O?,
      lastMetadata: OnboardingMetadataStore? = null,
    ) : this(
      parser = token.resultDecoder,
      nodeRef = token,
      nodeId = token.nodeId,
      resultBundle = result?.let(token.resultEncoder::encode),
      lastMetadata = lastMetadata,
    )

    override val type: String = TYPE_SUCCESS

    override fun <OO> resolve(parser: WithResultDecoder<PersistableBundle, OO>): Success<OO> =
      resolve(parser.resultDecoder)

    override fun <OO> resolve(parser: Encoder<PersistableBundle, OO>): Success<OO> =
      Success(parser, nodeRef, nodeId, resultBundle, lastMetadata)

    override fun equals(other: Any?) =
      other is Success<*> &&
        other refersTo this &&
        other.resultBundle contentDeepEquals resultBundle

    override fun hashCode() =
      Objects.hash(nodeComponent, nodeName, nodeId, resultBundle.generateHash())

    override fun toString() =
      "OnboardingTaskState.Success(${identity()}, result=${resultBundle?.forceToString()}, lastMetadata=$lastMetadata)"
  }

  /**
   * Represents the failed state of an onboarding task.
   *
   * @param errorMessage The error message describing the failure.
   * @param result The result associated with the failed task, if available.
   */
  @OptIn(InternalOnboardingApi::class)
  data class Failure<out O>
  @InternalOnboardingApi
  constructor(
    override val parser: Encoder<PersistableBundle, O>,
    override val nodeRef: NodeRef,
    override val nodeId: NodeId,
    private val resultBundle: PersistableBundle?,
    override val lastMetadata: OnboardingMetadataStore? = null,
    val errorMessage: IString,
  ) : Completed<O>(resultBundle, lastMetadata) {
    internal constructor(
      parser: Encoder<PersistableBundle, O>,
      nodeRef: NodeRef,
      bundle: PersistableBundle,
    ) : this(
      parser = parser,
      nodeRef = nodeRef,
      nodeId = bundle.nodeId,
      resultBundle = bundle.getPersistableBundle(EXTRA_RESULT),
      lastMetadata = bundle.getPersistable(EXTRA_LAST_METADATA, OnboardingMetadataStore),
      errorMessage =
        bundle.getString(EXTRA_ERROR)?.let { SensitiveString(it) }
          ?: NonSensitiveString(FAILED_TO_PARSE_ERROR_MESSAGE),
    )

    constructor(
      token: OnboardingTaskToken<O>,
      result: O?,
      errorMessage: IString,
      lastMetadata: OnboardingMetadataStore? = null,
    ) : this(
      parser = token.resultDecoder,
      nodeRef = token,
      nodeId = token.nodeId,
      resultBundle = result?.let(token.resultEncoder::encode),
      lastMetadata = lastMetadata,
      errorMessage = errorMessage,
    )

    override val type: String = TYPE_FAILED

    @InternalOnboardingApi
    override fun toPersistableBundle(): PersistableBundle =
      super.toPersistableBundle().apply { putString(EXTRA_ERROR, errorMessage.value) }

    override fun <OO> resolve(parser: WithResultDecoder<PersistableBundle, OO>): Failure<OO> =
      resolve(parser.resultDecoder)

    override fun <OO> resolve(parser: Encoder<PersistableBundle, OO>): Failure<OO> =
      Failure(parser, nodeRef, nodeId, resultBundle, lastMetadata, errorMessage)

    override fun equals(other: Any?) =
      other is Failure<*> &&
        other refersTo this &&
        other.resultBundle contentDeepEquals resultBundle &&
        other.errorMessage == errorMessage

    override fun hashCode() =
      Objects.hash(nodeComponent, nodeName, nodeId, resultBundle.generateHash(), errorMessage)

    override fun toString() =
      "OnboardingTaskState.Failure(${identity()}, errorMessage=$errorMessage, result=${resultBundle?.forceToString()})"

    companion object {
      /** Construct a [Failure] state. */
      operator fun invoke(
        nodeRef: NodeRef,
        nodeId: NodeId,
        errorMessage: IString,
        resultBundle: PersistableBundle? = null,
      ): Failure<Nothing> =
        Failure(
          nodeRef = nodeRef,
          nodeId = nodeId,
          errorMessage = errorMessage,
          resultBundle = resultBundle,
          lastMetadata = null,
          parser = { error("No Result provided") },
        )

      /** Construct a [Failure] state. */
      operator fun invoke(
        nodeRef: UniqueNodeRef,
        errorMessage: IString,
        resultBundle: PersistableBundle? = null,
      ): Failure<Nothing> =
        invoke(
          nodeRef = nodeRef,
          nodeId = nodeRef.nodeId,
          errorMessage = errorMessage,
          resultBundle = resultBundle,
        )
    }
  }

  companion object {
    internal const val TYPE_IN_PROGRESS = "InProgress"
    internal const val TYPE_SUCCESS = "Success"
    internal const val TYPE_FAILED = "Failed"
    internal const val EXTRA_TYPE = "${OnboardingTaskToken.NAMESPACE}.TYPE"
    internal const val EXTRA_ERROR = "${OnboardingTaskToken.NAMESPACE}.ERROR"
    internal const val EXTRA_RESULT = "${OnboardingTaskToken.NAMESPACE}.RESULT"
    internal const val EXTRA_LAST_METADATA = "${OnboardingTaskToken.NAMESPACE}.LAST_METADATA"
  }
}

/**
 * Erased variant of the [UnresolvedOnboardingTaskState] which loses output type data in favour of
 * being persistable and serializable.
 */
class ErasedOnboardingTaskState internal constructor(private val bundle: PersistableBundle) :
  UnresolvedOnboardingTaskState<Nothing>, Parcelable by bundle {
  override val nodeId: NodeId
    get() = bundle.nodeId

  override val nodeName: String
    get() = checkNotNull(bundle.getString(OnboardingTaskToken.EXTRA_NODE))

  override val nodeComponent: String
    get() = checkNotNull(bundle.getString(OnboardingTaskToken.EXTRA_COMPONENT))

  private val type: String
    get() = checkNotNull(bundle.getString(OnboardingTaskState.EXTRA_TYPE))

  @InternalOnboardingApi
  override fun toPersistableBundle(): PersistableBundle = PersistableBundle(bundle)

  override fun toErased(): ErasedOnboardingTaskState = this

  override fun <OO> resolve(parser: Encoder<PersistableBundle, OO>): OnboardingTaskState<OO> {
    return when (type) {
      OnboardingTaskState.TYPE_IN_PROGRESS ->
        OnboardingTaskState.InProgress(parser, asNodeRef(), bundle)

      OnboardingTaskState.TYPE_SUCCESS -> OnboardingTaskState.Success(parser, asNodeRef(), bundle)
      OnboardingTaskState.TYPE_FAILED -> OnboardingTaskState.Failure(parser, asNodeRef(), bundle)
      else -> error("Unknown OnboardingTaskState(type=$type)")
    }
  }

  override fun <OO> resolve(
    parser: WithResultDecoder<PersistableBundle, OO>
  ): OnboardingTaskState<OO> = resolve(parser.resultDecoder)

  override fun equals(other: Any?) =
    other is ErasedOnboardingTaskState && bundle contentDeepEquals other.bundle

  override fun hashCode() = bundle.generateHash()

  override fun toString(): String {
    return "ErasedOnboardingTaskState(type=$type identity=${identity()} lastMetadata=$lastMetadata)"
  }

  override val isCompleted
    get() = type == OnboardingTaskState.TYPE_SUCCESS || type == OnboardingTaskState.TYPE_FAILED

  override val lastMetadata: OnboardingMetadataStore?
    get() = bundle.getPersistable(EXTRA_LAST_METADATA, OnboardingMetadataStore)

  /** @see PersistableBundle.isTooLargeForTransaction */
  @InternalOnboardingApi
  fun isTooLargeForTransaction() = getResultBundle()?.isTooLargeForTransaction() ?: false

  /** @see PersistableBundle.splitForTransaction */
  @InternalOnboardingApi
  fun splitForTransaction(): List<ByteArray> {
    val resultBundle =
      requireNotNull(getResultBundle()) { "The task state {$this} doesn't have any result." }
    return resultBundle.splitForTransaction()
  }

  /**
   * Restores the full data for the "pending state".
   *
   * The state will no longer be a "pending state" after calling this.
   *
   * @see toPendingState
   * @see isPendingState
   */
  @OptIn(InternalOnboardingApi::class)
  fun restoreFullDataForPendingState(fullData: PersistableBundle): ErasedOnboardingTaskState {
    require(isPendingState()) { "This {$this} is not a pending state." }
    val bundle =
      toPersistableBundle().apply {
        removePendingBundleExtras()
        putPersistableBundle(EXTRA_RESULT, fullData)
      }
    return ErasedOnboardingTaskState(bundle)
  }

  /**
   * Returns a "pending state" representation of the state.
   *
   * This signals the receiver to fetch the full result separately that is too large for binder
   * transaction. The transformation is done by replacing the result bundle with the pending bundle
   * extras.
   *
   * Once the full result is fetched, the original state can be restored with
   * [restoreFullDataForPendingState].
   *
   * This should only be called when it is a [OnboardingTaskState.Completed] state that is
   * [isTooLargeForTransaction]. Otherwise an [IllegalArgumentException] is thrown.
   *
   * @see isPendingState
   */
  @InternalOnboardingApi
  fun toPendingState(): ErasedOnboardingTaskState {
    require(isTooLargeForTransaction()) {
      "The task state {$this} is not too large for transaction."
    }
    val resultBundle =
      requireNotNull(getResultBundle()) { "The task state {$this} doesn't have any result." }
    val bundle =
      toPersistableBundle().apply {
        // Need to keep non-result extras for state parsing.
        putPersistableBundle(EXTRA_RESULT, resultBundle.toPendingBundle())
      }
    return ErasedOnboardingTaskState(bundle)
  }

  /**
   * Returns if this is a "pending state".
   *
   * @see toPendingState
   */
  @OptIn(InternalOnboardingApi::class)
  fun isPendingState() = getResultBundle()?.isPendingBundle() ?: false

  /** @see PersistableBundle.getPendingDataTotalChunks */
  @InternalOnboardingApi
  fun getPendingDataTotalChunks(): Int {
    require(isPendingState()) { "This {$this} is not a pending state." }
    val resultBundle =
      requireNotNull(getResultBundle()) { "The task state {$this} doesn't have any result." }
    return resultBundle.getPendingDataTotalChunks()
  }

  /** @see PersistableBundle.getPendingDataSizeInBytes */
  @InternalOnboardingApi
  fun getPendingDataSizeInBytes(): Int {
    require(isPendingState()) { "This {$this} is not a pending state." }
    val resultBundle =
      requireNotNull(getResultBundle()) { "The task state {$this} doesn't have any result." }
    return resultBundle.getPendingDataSizeInBytes()
  }

  /** @see PersistableBundle.getPendingDataIdentifier */
  @InternalOnboardingApi
  fun getPendingDataIdentifier(): Int {
    require(isPendingState()) { "This {$this} is not a pending state." }
    val resultBundle =
      requireNotNull(getResultBundle()) { "The task state {$this} doesn't have any result." }
    return resultBundle.getPendingDataIdentifier()
  }

  @InternalOnboardingApi
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun getResultBundle() = toPersistableBundle().getPersistableBundle(EXTRA_RESULT)

  companion object CREATOR :
    PersistableDeserializer<ErasedOnboardingTaskState>,
    Parcelable.Creator<ErasedOnboardingTaskState> {
    override fun createFromParcel(parcel: Parcel): ErasedOnboardingTaskState =
      ErasedOnboardingTaskState(PersistableBundle.CREATOR.createFromParcel(parcel))

    override fun newArray(size: Int): Array<ErasedOnboardingTaskState?> = arrayOfNulls(size)

    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): ErasedOnboardingTaskState = ErasedOnboardingTaskState(persistableBundle)
  }
}

/**
 * Represents the state of an onboarding task.
 *
 * @param O The type of the result associated with the task.
 */
sealed interface UnresolvedOnboardingTaskState<out O> : UniqueNodeRef, Persistable, StableEquality {

  /** Convert this token to a [PersistableBundle]. */
  @InternalOnboardingApi override fun toPersistableBundle(): PersistableBundle

  /**
   * Converts this token to [ErasedOnboardingTaskState] token which loses contract reference in
   * order to become persistable and serializable.
   */
  @OptIn(InternalOnboardingApi::class)
  fun toErased(): ErasedOnboardingTaskState = ErasedOnboardingTaskState(toPersistableBundle())

  /** Converts this token to [OnboardingTaskState] token by associating it with a given [parser]. */
  fun <OO> resolve(parser: WithResultDecoder<PersistableBundle, OO>): OnboardingTaskState<OO>

  /** Converts this token to [OnboardingTaskState] token by associating it with a given [parser]. */
  fun <OO> resolve(parser: Encoder<PersistableBundle, OO>): OnboardingTaskState<OO>

  /** Whether the task has completed. */
  val isCompleted: Boolean

  /**
   * The last metadata emitted by a completed task.
   *
   * Returns `null` if the task is not completed.
   */
  val lastMetadata: OnboardingMetadataStore?
}

const val ERROR_FOREIGN_TASK_MANAGER_TOKEN = "Provided token was spawned by a foreign task manager."
const val ERROR_TASK_CANNOT_BE_FOUND = "Task cannot be found."
const val ERROR_METADATA_CANNOT_BE_FOUND = "Metadata cannot be found."
const val ERROR_TASK_CANNOT_BE_INTERRUPTED = "Task cannot be interrupted."
const val ERROR_RUNNING_TASK = "Task execution failed with an exception."
const val ERROR_FAILED_BIND_TASK_SERVICE = "Failed to bind to the task service."
const val ERROR_SERVICE_DIED = "Task execution failed because the task service died."
const val FAILED_TO_PARSE_ERROR_MESSAGE = "Failed to parse error message in the bundle."
