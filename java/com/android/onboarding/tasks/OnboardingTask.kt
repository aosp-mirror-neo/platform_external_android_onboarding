package com.android.onboarding.tasks

import android.content.Context
import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.InterruptReason
import com.android.onboarding.metadata.IString
import com.android.onboarding.metadata.NonSensitiveString
import com.android.onboarding.metadata.OnboardingMetadata
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.metadata.SensitiveString
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.nodes.UniqueNodeRef
import com.android.onboarding.nodes.refersTo
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableBundle
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableMapDeserializer
import com.android.onboarding.utils.persistable.PersistableMapSerializer
import com.android.onboarding.utils.persistable.PersistablePersistableBundle
import com.android.onboarding.utils.persistable.PersistableSerializer
import com.android.onboarding.utils.persistable.PersistableString
import com.android.onboarding.versions.OnboardingChanges
import com.android.onboarding.versions.changes.DEFAULT_INTERRUPT_SUPPORT
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.lang.ref.WeakReference
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * An abstract base class for defining onboarding tasks in the onboarding process. This can be
 * thought as an equivalent to the Activity in terms of representing the actual work being done by
 * the associated contract.
 *
 * @param I The type representing the input arguments for the onboarding task.
 * @param O The type representing the result of the onboarding task.
 * @param C The type representing the onboarding contract which can work with this task.
 */
@OptIn(InternalOnboardingApi::class)
abstract class OnboardingTask<I, O, C : BaseOnboardingTaskContract<I, O>> :
  UniqueNodeRef, LifecycleOwner {
  private inline val tag
    get() = "OnboardingTask(${identity()})"

  /** The contract implemented by this task. */
  abstract val contract: C

  @Volatile @InternalOnboardingApi var contextRef: WeakReference<Context>? = null

  override fun toString(): String = "OnboardingTask(${identity()})"

  final override val nodeComponent: String
    get() = contract.nodeComponent

  final override val nodeName: String
    get() = contract.nodeName

  final override var nodeId: NodeId = UNKNOWN_NODE_ID
    private set

  private val onboardingLifecycle by lazy { OnboardingTaskLifecycle(this) }
  override val lifecycle: Lifecycle
    get() = onboardingLifecycle

  private var _lifecycleScope: CoroutineScope? = null
  protected val lifecycleScope: CoroutineScope
    get() = checkNotNull(_lifecycleScope) { "LifecycleScope is not initialized!" }


  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  var isRunning: Boolean = false
    set(value) {
      if (value) {
        onboardingLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
      } else {
        onboardingLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
      }
      field = value
    }

  private var taskContext: OnboardingTaskContext? = null

  /** Provided [OnboardingTaskManager] the implementations can use to launch other tasks. */
  protected val taskManager: OnboardingTaskManager
    get() =
      checkNotNull(taskContext?.onboardingTaskManager) {
        "OnboardingTaskManager is not initialized!"
      }

  /** Has task's [onCreate] method been invoked. */
  val isCreated
    get() = taskContext != null

  /** Create and initialise the task. */
  @InternalOnboardingApi
  fun onCreate(context: OnboardingTaskContext) {
    require(context.refersTo(contract)) {
      "Provided context $context does not refer to the task contract $contract"
    }
    this.taskContext = context
    this.nodeId = context.nodeId
    metadataStore.clear()
    isRunning = false
    val newScope =
      OnboardingTaskCoroutineScopeProvider.get(
        /* lifecycle = */ lifecycle,
        /* coroutineContext = */ context.coroutineContext +
          SupervisorJob() +
          CoroutineName(identity()),
      )
    val currentScope = lifecycle.internalScopeRef.get()
    if (lifecycle.internalScopeRef.compareAndSet(currentScope, newScope)) {
      OnboardingTaskCoroutineScopeProvider.register(newScope)
      _lifecycleScope = newScope
    }
    onboardingLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
  }

  /**
   * Parse [input] and execute task.
   *
   * Main internal entrypoint for the task execution.
   *
   * This method will return task failure if called before [onCreate].
   */
  suspend fun execute(
    context: Context,
    input: PersistableBundle,
    timeout: Duration = contract.timeout,
  ): OnboardingTaskState<O> {
    if (!isCreated) {
      return failure(
        errorMessage = NonSensitiveString("Task execution started before it was created."),
        result = null,
      )
    }
    contextRef = WeakReference(context)
    return executeTask(context, input, timeout)
  }

  /**
   * Destroy the task and clear its state.
   *
   * Task instance should not be reused after this point.
   */
  @InternalOnboardingApi
  fun onDestroy() {
    isRunning = false
    onboardingLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    onboardingLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  }

  /** Any exceptions thrown on any coroutines registered using [async]. */
  private val thrownException = AtomicReference<Throwable>(null)

  /**
   * Interrupt the main execution of runTask.
   *
   * As long as standard task coroutine practices are followed (e.g. registering jobs with the task)
   * then they will all be terminated, and the task will fail with the given exception.
   */
  @InternalOnboardingApi
  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  fun interruptWithException(throwable: Throwable) {
    thrownException.set(throwable)
  }

  /** Coroutines which will need to finish before the task completes. */
  private val asyncCoroutines = mutableListOf<Job>()


  /** Wraps [block] with appropriate error handling which causes exceptions to fail the task. */
  @CanIgnoreReturnValue
  @JvmOverloads
  fun <T> async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
  ): Deferred<T> =
    lifecycleScope
      .async(context, start) {
        try {
          this.block()
        } catch (e: Exception) {
          // We communicate this back to the main thread so it'll be thrown
          thrownException.set(e)
          throw IllegalStateException("Failed async due to exception", e)
        }
      }
      .also { recordTaskBlockingJob(it) }

  /**
   * Record that the passed in [job] must finish before the task result can be returned.
   *
   * If the task finishes successfully, then all recorded [job]s will have [Deferred.await] called.
   *
   * If an exception is thrown either on the main task thread, or on a recorded [job], then all
   * other recorded [job]s will be cancelled.
   *
   * This should be created using [CoroutineScope.async].
   *
   * @see async
   */
  protected fun recordTaskBlockingJob(job: Job) {
    asyncCoroutines.add(job)
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  val metadataStore = OnboardingMetadataStore()

  /** Report task success state. */
  protected fun success(result: O? = null): OnboardingTaskState<O> =
    OnboardingTaskState.Success(
      parser = contract.resultDecoder,
      nodeRef = contract,
      nodeId = nodeId,
      resultBundle = result?.let(contract.resultEncoder::encode),
      lastMetadata = if (metadataStore.isEmpty) null else metadataStore,
    )

  /** Internal version of [success]. */
  internal fun successInternal(result: O? = null): OnboardingTaskState<O> = success(result)

  /** Report task failure state. */
  protected fun failure(errorMessage: IString, result: O? = null): OnboardingTaskState<O> =
    OnboardingTaskState.Failure(
      parser = contract.resultDecoder,
      nodeRef = contract,
      nodeId = nodeId,
      resultBundle = result?.let(contract.resultEncoder::encode),
      lastMetadata = if (metadataStore.isEmpty) null else metadataStore,
      errorMessage = errorMessage,
    )

  /** Internal version of [failure]. */
  internal fun failureInternal(errorMessage: IString, result: O? = null): OnboardingTaskState<O> =
    failure(errorMessage, result)

  protected class NodeFailureError
  @InternalOnboardingApi
  constructor(override val message: String, val data: Any?) : Error(message)

  /**
   * Fail this onboarding node.
   *
   * @param message a message describing the failure.
   * @param result an optional result data associated with the failure.
   */
  protected fun failNode(message: String, result: O? = null): Nothing {
    throw NodeFailureError(message, result)
  }

  /**
   * An override point for specialised [OnboardingTask] bases to route based on incoming [input].
   */
  @InternalOnboardingApi
  protected open suspend fun executeTask(
    context: Context,
    input: PersistableBundle,
    timeout: Duration,
  ): OnboardingTaskState<O> =
    runTaskForState(
      context = context,
      input = contract.inputDecoder.encode(input),
      timeout = timeout,
    )

  /**
   * Runs an onboarding task.
   *
   * Note that all invocations of this method will report an [OnboardingTaskState] with
   * [UNKNOWN_NODE_ID][UNKNOWN_NODE_ID].
   *
   * @param context the application context for the execution.
   * @param input The input arguments for the task.
   * @param timeout The timeout duration. If not specified, the timeout defined by the implementing
   *   contract will be used.
   * @return An [OnboardingTaskState] representing the result of the task execution. If the task
   *   times out, a failed result will be returned.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  suspend fun runTaskForState(
    context: Context,
    input: I,
    timeout: Duration = contract.timeout,
  ): OnboardingTaskState<O> {
    // Needed until we merge ContextAwareOnboardingTask into OnboardingTask.
    contextRef = WeakReference(context)
    if (!contract.validate(input)) {
      return failure(
        errorMessage =
          SensitiveString(
            "Task argument doesn't align with the " +
              "OnboardingTaskContract($contract) definition. " +
              "Failed returned immediately before execution."
          ),
        result = null,
      )
    }
    // Resets the metadata flow when a task is run. This is to protect the case when a task
    // singleton is being re-run.
    metadataStore.clear()
    taskContext?.taskStateManager?.setMetadata(this, metadataStore)
    return startExecution(timeout) { runTask(context, input) }
  }

  /** Start the [execution] and handle common failures. */
  @InternalOnboardingApi
  protected suspend fun startExecution(
    timeout: Duration = contract.timeout,
    execution: suspend () -> O,
  ): OnboardingTaskState<O> {
    isRunning = true
    val task = this
    thrownException.set(null)

    return try {
      withTimeout(timeout.toMillis()) {
        // We run the main task logic in one coroutine
        val mainExecution = lifecycleScope.async { execution() }.also { recordTaskBlockingJob(it) }
        // And have a second to watch for exceptions thrown anywhere
        val coroutineWatcher =
          task
            .async(CoroutineName("OnboardingTaskExecutionWatcher(${identity()})")) {
              while (true) {
                val latestException = thrownException.get()
                if (latestException != null) throw latestException
                delay(1.seconds)
              }
            }
            .also { recordTaskBlockingJob(it) }

        // Wait for either mainExecution or coroutineWatcher to finish
        val selected = select {
          // We're just trying to extract exceptions - we get the actual result below
          val unused1 = mainExecution.onAwait { it as Any }
          val unused2 = coroutineWatcher.onAwait {}
        }

        // If we reached here then no exception was thrown
        @Suppress("UNCHECKED_CAST") val mainResult = selected as O

        success(mainResult)
      }
    } catch (e: NodeFailureError) {
      @Suppress("UNCHECKED_CAST") failure(SensitiveString(e.message), e.data as O?)
    } catch (e: TimeoutCancellationException) {
      failure(SensitiveString("Task timed out: " + e.message))
    } catch (e: Exception) {
      failure(SensitiveString("Task failed: ${e.message} (${e.stackTraceToString()})"))
    } finally {
      withContext(NonCancellable) {
        while (asyncCoroutines.isNotEmpty()) {
          val job = asyncCoroutines.removeAt(0)
          if (!job.isCompleted) {
            Log.d(tag, "Cancelling coroutine job $job")
            job.cancelAndJoin()
          } else {
            Log.v(tag, "Not cancelling already completed coroutine job $job")
          }
        }
      }
      isRunning = false
    }
  }

  /**
   * Abstract method to be implemented by child classes. Executes the onboarding task
   * asynchronously.
   *
   * @param context the application context for the execution.
   * @param input The input arguments for the task.
   * @return The result of the onboarding task.
   */
  protected abstract suspend fun runTask(context: Context, input: I): O

  /** See [updateMetadata]. */
  inline fun <reified T : OnboardingMetadata> updateMetadata(metadata: T) {
    updateMetadata(T::class.java, metadata)
  }

  /**
   * Updates [metadata].
   *
   * Note that you can update any metadata included in the "metadata" attribute of the associated
   * OnboardingNode annotation.
   *
   * You can only update metadata during [runTask]. This will no-op otherwise. If applicable, you
   * are responsible to update the metadata before succeeding or failing gracefully.
   *
   * @param metadata The updated metadata.
   * @see clearMetadata
   */
  fun <T : OnboardingMetadata> updateMetadata(metadataType: Class<T>, metadata: T) {

    // API. We will
    //  then emit as a single metadata object, with dampening to avoid emitting multiple times in
    //  a short period of time.
    if (!isRunning) {
      Log.w(
        tag,
        "Task ($this) can only update metadata when it is running. Ignoring update: ${metadata.toString(contextRef?.get())}",
      )
      return
    }
    Log.d(tag, "Task ($this) updating metadata: ${metadata.toString(contextRef?.get())}")

    metadataStore.put(metadataType, metadata)
    taskContext?.taskStateManager?.setMetadata(this, metadataStore)
    maybeEmitMetadataToTaskStateManager(metadataStore)
  }

  /**
   * Emits metadata to [OnboardingTaskStateManager].
   *
   * This is only done if the feature is enabled, and if the [taskContext] is initialized.
   */
  private fun maybeEmitMetadataToTaskStateManager(metadata: OnboardingMetadataStore) {
    taskContext?.taskStateManager?.emitMetadata(this, metadata)
  }

  /** Clears a previously-set metadata set using [updateMetadata]. */
  fun <T : OnboardingMetadata> clearMetadata(metadataType: Class<T>) {
    metadataStore.clear(metadataType)
    taskContext?.taskStateManager?.setMetadata(this, metadataStore)
    maybeEmitMetadataToTaskStateManager(metadataStore)
  }

  /**
   * A caller has requested that the task be interrupted for [reason].
   *
   * See [OnboardingTaskManager.interrupt] for client expectations of interruption.
   *
   * This can be overridden to provide custom behavior. By default, unexpected interruptions (those
   * not declared in the contract) will be ignored, expected [InterruptReason.USER_CANCELLED] and
   * [InterruptReason.RESULT_NO_LONGER_NEEDED] reasons will result in the task being cancelled with
   * a failure result.
   */
  @OptIn(InternalOnboardingApi::class)
  open suspend fun onInterrupt(reason: InterruptReason) {
    if (
      taskContext?.onboardingChanges?.currentProcessSupportsChange(DEFAULT_INTERRUPT_SUPPORT) !=
        true
    ) {
      Log.e(tag, "Unhandled interrupt with reason $reason ($this)")
      return
    }

    if (!contract.canAcceptInterrupt(reason)) {
      Log.e(tag, "Unexpected interrupt with reason $reason ($this)")
      return
    }

    if (
      reason == InterruptReason.RESULT_NO_LONGER_NEEDED || reason == InterruptReason.USER_CANCELLED
    ) {
      interruptWithException(IllegalStateException("Interrupted with reason $reason"))
      return
    }

    Log.e(tag, "Unhandled interrupt with reason $reason ($this)")
  }

  private val persistableState = PersistableStateContainer(::isRunning)

  /** Restore previously persisted [persistableState]. */
  @InternalOnboardingApi
  protected fun restoreState(state: PersistableBundle) {
    persistableState.fromPersistableBundle(state)
  }

  /** Serialize the current [persistableState]. */
  @InternalOnboardingApi
  protected fun saveState(): PersistableBundle = persistableState.toPersistableBundle()

  /**
   * Initialize a persistable state delegate property.
   *
   * Values written via this delegate will be included in [saveState] and [restoreState], thus
   * allowing to extract and load back the state between task instances.
   *
   * IMPORTANT: Calls of this method are only allowed during class initialisation and for top-level
   * class properties. It will throw an error when invoked at all other times.
   *
   * @param serializer [PersistableSerializer] to use during [saveState].
   * @param deserializer [PersistableDeserializer] to use during [restoreState].
   * @param initialValue optional value to initialize the delegate with. If [nullable]=false and no
   *   [initialValue] is provided, the property will behave as if it were `lateinit var`.
   * @param key optional key to use for storing persisted state values for this property. If not
   *   provided, property name will be used.
   * @param nullable is this property nullable and should return null when no value is set yet.
   */
  @InternalOnboardingApi
  protected fun <T> state(
    serializer: PersistableSerializer<T>,
    deserializer: PersistableDeserializer<T>,
    initialValue: T?,
    key: String?,
    nullable: Boolean,
  ): PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>> =
    PropertyDelegateProvider { _: Any?, property: KProperty<*> ->
      val id = key ?: property.name
      persistableState.setup(id, serializer, deserializer, initialValue, nullable)
    }

  /**
   * Initialize a persistable state delegate property.
   *
   * Values written via this delegate will be included in [saveState] and [restoreState], thus
   * allowing to extract and load back the state between task instances.
   *
   * IMPORTANT: Calls of this method are only allowed during class initialisation and for top-level
   * class properties. It will throw an error when invoked at all other times.
   *
   * @param serializer [PersistableSerializer] to use during [saveState].
   * @param deserializer [PersistableDeserializer] to use during [restoreState].
   * @param initialValue optional value to initialize the delegate with. If [T] is nullable and no
   *   [initialValue] is provided, the property will behave as if it were `lateinit var`.
   * @param key optional key to use for storing persisted state values for this property. If not
   *   provided, property name will be used.
   */
  protected inline fun <reified T> state(
    serializer: PersistableSerializer<T>,
    deserializer: PersistableDeserializer<T>,
    initialValue: T? = null,
    key: String? = null,
  ) = state(serializer, deserializer, initialValue, key, nullable = null is T)

  /**
   * Initialize a persistable state delegate property.
   *
   * Values written via this delegate will be included in [saveState] and [restoreState], thus
   * allowing to extract and load back the state between task instances.
   *
   * IMPORTANT: Calls of this method are only allowed during class initialisation and for top-level
   * class properties. It will throw an error when invoked at all other times.
   *
   * @param deserializer [PersistableDeserializer] to use during [restoreState].
   * @param initialValue optional value to initialize the delegate with. If [T] is nullable and no
   *   [initialValue] is provided, the property will behave as if it were `lateinit var`.
   * @param key optional key to use for storing persisted state values for this property. If not
   *   provided, property name will be used.
   */
  protected inline fun <reified T : Persistable?> state(
    deserializer: PersistableDeserializer<T>,
    initialValue: T? = null,
    key: String? = null,
  ) =
    state({ it?.toPersistableBundle() ?: PersistableBundle.EMPTY }, deserializer, initialValue, key)

  private class PersistableStateContainer(private val writeLocked: () -> Boolean) : Persistable {
    private val data = mutableMapOf<String, PersistableState<*>>()

    @Suppress("UNCHECKED_CAST") // Unerasing controlled generic type
    private fun <T> getState(key: String) = data.getValue(key) as PersistableState<T>

    operator fun <T> get(key: String): T = getState<T>(key).get()

    operator fun <T> set(key: String, value: T): Unit = getState<T>(key).update(value)

    fun <T> setup(
      key: String,
      serializer: PersistableSerializer<T>,
      deserializer: PersistableDeserializer<T>,
      value: T?,
      nullable: Boolean,
    ): ReadWriteProperty<Any?, T> {
      check(!writeLocked()) { "Adding new state containers is only allowed during task creation!" }
      check(data[key] == null) { "State container for key $key already exists!" }
      val state = PersistableState(serializer, deserializer, nullable, key, value)
      data[key] = state
      return state
    }

    override fun toPersistableBundle() =
      PersistableMapSerializer(keySerializer = PersistableString, valueSerializer = Persistable)
        .toPersistableBundle(data)

    fun fromPersistableBundle(bundle: PersistableBundle) {
      val map =
        PersistableMapDeserializer(
            keyDeserializer = PersistableString::rawFromPersistableBundle,
            valueDeserializer = PersistablePersistableBundle::rawFromPersistableBundle,
          )
          .fromPersistableBundle(bundle)
      for ((key, value) in map) {
        getState<Nothing>(key).fromPersistableBundle(value)
      }
    }

    private class PersistableState<T>(
      private val serializer: PersistableSerializer<T>,
      private val deserializer: PersistableDeserializer<T>,
      private val nullable: Boolean,
      private val key: String,
      private var value: T?,
    ) : ReadWriteProperty<Any?, T>, Persistable {
      private val lock = ReentrantLock()

      fun update(value: T) {
        try {
          lock.lock()
          this.value = value
        } finally {
          lock.unlock()
        }
      }

      fun get(): T =
        value
          ?: if (nullable) {
            @Suppress("UNCHECKED_CAST") // T is specified to be nullable
            null as T
          } else {
            error(
              "PersistableState[$key] is not marked as nullable and does not contain a value during access!"
            )
          }

      override fun toPersistableBundle(): PersistableBundle =
        value?.let(serializer::toPersistableBundle)
          ?: PersistableBundle { putString(key, "${null}") }

      fun fromPersistableBundle(bundle: PersistableBundle) {
        if (bundle.getString(key) != "${null}") {
          update(deserializer.fromPersistableBundle(bundle))
        }
      }

      override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

      override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = update(value)
    }
  }

  /**
   * Contextual data needed for [OnboardingTask] to function.
   *
   * @property nodeRef A [UniqueNodeRef] assigned to this task.
   */
  class OnboardingTaskContext
  constructor(
    val nodeRef: UniqueNodeRef,
    val taskStateManager: OnboardingTaskStateManager,
    val coroutineContext: CoroutineContext,
    val onboardingChanges: OnboardingChanges,
    val onboardingTaskManager: OnboardingTaskManager,
  ) : UniqueNodeRef by nodeRef
}

/**
 * A variation of [OnboardingTask] which provides context at class level during execution.
 *
 * @see OnboardingTask
 */
@OptIn(InternalOnboardingApi::class)
abstract class ContextAwareOnboardingTask<I, O, C : OnboardingTaskContract<I, O>> :
  OnboardingTask<I, O, C>() {
  /**
   * Application [Context] injected before each invocation of [runTask]. Accessing it outside of the
   * [runTask] invocation will likely throw an error.
   */
  protected val context: Context
    get() = checkNotNull(contextRef?.get()) { "Context accessed before it was initialised." }

  /**
   * [Context] free version of [runTask] which is always guaranteed to be invoked after [context]
   * property is initialized.
   *
   * @see runTask
   */
  protected abstract suspend fun runTask(input: I): O

  final override suspend fun runTask(context: Context, input: I): O = runTask(input)
}
