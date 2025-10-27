package com.android.onboarding.tasks

import android.content.Context
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.datastore.DefaultOnboardingDataStore
import com.android.onboarding.datastore.OnboardingDataStore
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.process.KeepAliveManager
import com.android.onboarding.tasks.security.TaskPermissionManager
import com.android.onboarding.tasks.service.OnboardingTaskServiceServer
import com.android.onboarding.tasks.storage.DATA_STORE_KEY_PREFIX
import com.android.onboarding.tasks.storage.taskDataStore
import com.android.onboarding.utils.TimeSource
import com.android.onboarding.versions.DefaultOnboardingChanges
import com.android.onboarding.versions.OnboardingChanges
import java.util.function.Supplier
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlinx.coroutines.CoroutineScope

/**
 * This is a class for services that manage onboarding tasks.
 *
 * Clients should expose this binder, e.g. via a service, that allows clients to interact with the
 * onboarding task manager by binding to it and invoking tasks.
 *
 * Implementations are expected to bind it to the intent specified in
 * [OnboardingTaskContract.taskServiceIntent].
 *
 * This class also maintains the task states for the tasks that's executed by itself.
 *
 * You should **not** subclass this class. This is for use by the Onboarding Library only.
 *
 * As of [version] 1 this binder is only used to handle RPC communication. All executions are
 * delegated to [server].
 *
 * @property tasks A collection of suppliers for all tasks supported by this service binder. The
 *   supplier must supply a new instance of the tasks. Otherwise starting the same task multiple
 *   times will result in failures.
 * @property providedActivities all activity contracts which are allowed to be invoked via
 *   providers.
 * @property providedTasks all task contracts which are allowed to be invoked via providers.
 * @param component ref of the component hosting this binder.
 * @property server [OnboardingTaskServiceServer] instance handling all calls since [version] 1.
 * @see OnboardingTaskServiceServer
 */
/*
 * This class is essentially deprecated and only serves to expose [OnboardingTaskServiceServer].
 * Only legacy behaviour bugfixes should go to this class,
 * all new features should be implemented in [OnboardingTaskServiceServer].
 */
// Once it's not experimental we will use @SubclassOptInRequired(InternalOnboardingApi::class)
@OptIn(InternalOnboardingApi::class)
open class OnboardingTaskManagerServiceBinder(
  component: ComponentRef,
  contextProvider: Supplier<Context>,
  tasks: Collection<Supplier<out OnboardingTask<*, *, *>>>,
  taskPermissionManager: TaskPermissionManager,
  keepAliveManager: KeepAliveManager,
  coroutineScope: CoroutineScope,
  onboardingChangesSupplier: Supplier<OnboardingChanges> = Supplier {
    DefaultOnboardingChanges(contextProvider.get())
  },
  taskDataStore: OnboardingDataStore =
    DefaultOnboardingDataStore(
      contextProvider.get(),
      coroutineScope = coroutineScope,
      dataStore = contextProvider.get().taskDataStore,
      keyPrefix = DATA_STORE_KEY_PREFIX,
    ),
  providedActivities: Set<OnboardingActivityApiContract<*, *>> = setOf(),
  providedTasks: Set<OnboardingTaskContract<*, *>> = setOf(),
  taskStateManager: OnboardingTaskStateManager =
    OnboardingTaskStateManager(onboardingChangesSupplier.get(), taskDataStore, coroutineScope),
  crossProcessTaskManager: CrossProcessTaskManager =
    DefaultCrossProcessTaskManager(contextProvider, onboardingChangesSupplier.get()),
  timeSource: TimeSource = TimeSource.system(),
  otm: ReadWriteProperty<Any?, OnboardingTaskManager> = Delegates.notNull(),
) :
  DefaultOnboardingTaskManagerServiceBinder(
    component = component,
    contextProvider = contextProvider,
    tasks = tasks,
    taskPermissionManager = taskPermissionManager,
    keepAliveManager = keepAliveManager,
    coroutineContext = coroutineScope.coroutineContext,
    onboardingChangesSupplier = onboardingChangesSupplier,
    providedActivities = providedActivities,
    providedTasks = providedTasks,
    taskStateManager = taskStateManager,
    crossProcessTaskManager = crossProcessTaskManager,
    timeSource = timeSource,
    taskDataStore = taskDataStore,
    otm = otm,
    server =
      OnboardingTaskServiceServer(
        component = component,
        contextProvider = contextProvider,
        tasks = tasks,
        taskPermissionManager = taskPermissionManager,
        keepAliveManager = keepAliveManager,
        coroutineScope = coroutineScope,
        onboardingChangesSupplier = onboardingChangesSupplier,
        taskStateManager = taskStateManager,
        providedActivities = providedActivities,
        providedTasks = providedTasks,
        otm = otm,
      ),
  )
