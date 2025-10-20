package com.android.onboarding.control.chained

import android.os.PersistableBundle
import androidx.activity.result.ActivityResult
import androidx.annotation.VisibleForTesting
import com.android.onboarding.compatibility.compatGetBoolean
import com.android.onboarding.compatibility.compatPutBoolean
import com.android.onboarding.control.chained.ChainedInput.ActivityCallbackInput
import com.android.onboarding.control.chained.ChainedInput.RegularInput
import com.android.onboarding.control.chained.ChainedInput.UnknownInput
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.PersistableNodeRef
import com.android.onboarding.utils.persistable.ActivityResultPersistableDelegate
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableBundle
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable

/**
 * A wrapper over [Persistable] input which allows checking for non-standard values while still
 * being able to handle standard input values without caller API changes. Deserializing a given
 * [Persistable] via [ChainedInput] is non-intrusive and is equivalent to direct type
 * deserialization for the standard input values. "Standard" input here means the input a given
 * onboarding node would expect as defined by its generic type argument. Calling
 * [ChainedInput.RegularInput.resolve] is equivalent to deserializing underlying bundle without
 * going through [ChainedInput] at all. So in essence [ChainedInput] can be thought as a
 * serialization wrapper over a [PersistableBundle] representation of any given type that could
 * potentially not actually be that type and instead have some known side-channel data needed by the
 * onboarding entity receiving such input to continue its execution.
 *
 * Primarily meant for use with [OrchestratedTasks][com.android.onboarding.control.OrchestratedTask]
 * and [ChainedContracts][ChainedContract]. Nota that it is **NOT** meant to be used as an input to
 * a [ChainedContract], but rather as an input to the onboarding node that previously spawned a
 * [ChainedContract] and is now receiving a callback from it which naturally will never be the same
 * type as the entity's regular input type.
 *
 * @see RegularInput
 * @see UnknownInput
 * @see ActivityCallbackInput
 */
sealed interface ChainedInput : Persistable {
  /**
   * A [ChainedInput] representing a standard input value. standard input value here simply means
   * that no side channel input data was detected in the raw bundle and as such it should be
   * [resolved][resolve] just like a regular input would have been resolved directly. Do note that
   * this is not the same as [UnknownInput] since the latter signifies an existence of chained input
   * metadata which could not be interpreted by the current version of the library.
   */
  @JvmInline
  value class RegularInput(private val bundle: PersistableBundle) : ChainedInput {
    override fun toPersistableBundle() = bundle

    /** Resolve the underlying value into a proper type [T] using the provided [deserializer]. */
    fun <T> resolve(deserializer: PersistableDeserializer<T>) =
      deserializer.fromPersistableBundle(bundle)
  }

  /**
   * A [ChainedInput] which had an unknown internal [ChainedInput] type specified in the underlying
   * [PersistableBundle]. This usually indicates that the underlying [PersistableBundle] **DOES
   * NOT** contain a standard input and cannot be parsed into any known [ChainedInput] types. This
   * should not be confused with [RegularInput] which signifies that there was no chained input
   * metadata detected and as such the underlying bundle should be treated and resolved like a
   * regular input of the onboarding node receiving this.
   */
  @JvmInline
  value class UnknownInput(private val bundle: PersistableBundle) : ChainedInput {
    override fun toPersistableBundle() = bundle

    /** The type of this chained input which was unrecognised. */
    val type: String
      get() = checkNotNull(bundle.getString(KEY_TYPE))
  }

  /**
   * A [ChainedInput] representing a callback from a previous request to launch an activity for
   * result.
   *
   * Usually spawned by [ChainedContract.Activity] returns earlier in the node's lifecycle at which
   * point the node was paused and this input is meant to signal that the node should resume its
   * execution with the activity result it requested.
   */
  @JvmInline
  value class ActivityCallbackInput(private val bundle: PersistableBundle) : ChainedInput {
    constructor(
      session: PersistableBundle,
      source: NodeRef,
      data: ActivityResult,
      metadataStore: OnboardingMetadataStore? = null,
      failure: Boolean = false,
      state: PersistableBundle? = null,
    ) : this(
      PersistableBundle {
        putString(KEY_TYPE, TYPE_ACTIVITY_CALLBACK)
        compatPutBoolean(KEY_FAILURE, failure)
        putPersistableBundle(KEY_SESSION, session)
        putPersistableBundle(KEY_STATE, state)
        putPersistable(KEY_SOURCE, source, PersistableNodeRef)
        putPersistable(KEY_METADATA_STORE, metadataStore)
        putPersistable(KEY_DATA, data, ActivityResultPersistableDelegate)
      }
    )

    override fun toPersistableBundle() = bundle

    /** A session data from the initial callback request. */
    val session: PersistableBundle
      get() = checkNotNull(bundle.getPersistableBundle(KEY_SESSION))

    /**
     * The activity node that produced the result requested by the originating node. This is **NOT**
     * a reference to the node that spawned the initial request not the node that is being resumed.
     */
    val source: NodeRef
      get() = checkNotNull(bundle.getPersistable(KEY_SOURCE, PersistableNodeRef))

    /** [OnboardingMetadataStore] associated with the requesting node. */
    val metadataStore: OnboardingMetadataStore?
      get() = bundle.getPersistable(KEY_METADATA_STORE, OnboardingMetadataStore)

    /**
     * Result data provided by the launched activity. This is raw transfer layer representation of
     * the typed data the originating note has requested when it initially asked for an activity
     * result.
     */
    val data: ActivityResult
      get() = checkNotNull(bundle.getPersistable(KEY_DATA, ActivityResultPersistableDelegate))

    /** Indicates if the encapsulated result data represent failure of its source node. */
    val failure: Boolean
      get() = bundle.compatGetBoolean(KEY_FAILURE, defaultValue = false)

    /** Persisted state of the callback node. */
    val state: PersistableBundle?
      get() = bundle.getPersistableBundle(KEY_STATE)
  }

  /**
   * A [ChainedInput] representing a callback from a previous request to launch a task for result.
   * Usually spawned by [ChainedContract.Task] returns earlier in the node's lifecycle. [NodeRef]
   * implemented by this class refers to the task node that produced the result.
   */
  @JvmInline
  value class TaskCallbackInput(private val bundle: PersistableBundle) : ChainedInput {
    constructor(
      session: PersistableBundle,
      source: NodeRef,
      data: PersistableBundle,
      metadataStore: OnboardingMetadataStore? = null,
      failure: Boolean = false,
      state: PersistableBundle? = null,
    ) : this(
      PersistableBundle {
        putString(KEY_TYPE, TYPE_TASK_CALLBACK)
        compatPutBoolean(KEY_FAILURE, failure)
        putPersistableBundle(KEY_SESSION, session)
        putPersistableBundle(KEY_STATE, state)
        putPersistable(KEY_SOURCE, source, PersistableNodeRef)
        putPersistable(KEY_METADATA_STORE, metadataStore)
        putPersistableBundle(KEY_DATA, data)
      }
    )

    override fun toPersistableBundle() = bundle

    /** A session data from the initial callback request. */
    val session: PersistableBundle
      get() = checkNotNull(bundle.getPersistableBundle(KEY_SESSION))

    /**
     * The task node that produced the result requested by the originating node. This is **NOT** a
     * reference to the node that spawned the initial request not the node that is being resumed.
     */
    val source: NodeRef
      get() = checkNotNull(bundle.getPersistable(KEY_SOURCE, PersistableNodeRef))

    /** [OnboardingMetadataStore] associated with the requesting node. */
    val metadataStore: OnboardingMetadataStore?
      get() = bundle.getPersistable(KEY_METADATA_STORE, OnboardingMetadataStore)

    /** Result data provided by the launched task. */
    val data: PersistableBundle
      get() = checkNotNull(bundle.getPersistableBundle(KEY_DATA))

    /** Indicates if the encapsulated result data represent failure of its source node. */
    val failure: Boolean
      get() = bundle.compatGetBoolean(KEY_FAILURE, defaultValue = false)

    /** Persisted state of the callback node. */
    val state: PersistableBundle?
      get() = bundle.getPersistableBundle(KEY_STATE)
  }

  companion object : PersistableDeserializer<ChainedInput> {
    private const val TYPE_ACTIVITY_CALLBACK = "ActivityCallbackInput"
    private const val TYPE_TASK_CALLBACK = "TaskCallbackInput"
    @VisibleForTesting const val KEY_TYPE = "ci.TYPE"
    private const val KEY_SESSION = "ci.KEY_SESSION"
    private const val KEY_STATE = "ci.KEY_STATE"
    private const val KEY_SOURCE = "ci.KEY_SOURCE"
    private const val KEY_METADATA_STORE = "ci.KEY_METADATA_STORE"
    private const val KEY_DATA = "ci.KEY_DATA"
    private const val KEY_FAILURE = "ci.FAILURE"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): ChainedInput =
      when (persistableBundle.getString(KEY_TYPE)) {
        TYPE_ACTIVITY_CALLBACK -> ActivityCallbackInput(persistableBundle)
        TYPE_TASK_CALLBACK -> TaskCallbackInput(persistableBundle)
        null -> RegularInput(persistableBundle)
        else -> UnknownInput(persistableBundle)
      }
  }
}
