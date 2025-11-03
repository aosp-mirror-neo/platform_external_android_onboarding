package com.android.onboarding.versions.changes

import com.android.onboarding.versions.annotations.ChangeId
import com.android.onboarding.versions.annotations.ChangeRadius
import com.android.onboarding.versions.changes.Tags.NEXT
import com.android.onboarding.versions.changes.Tags.P10_DVT
import com.android.onboarding.versions.changes.Tags.P11_DVT
import com.android.onboarding.versions.changes.Tags.P11_EVT

@ChangeId(
  bugId = 354143470L,
  owner = "iancheng",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  available = "2025-01-08",
  tags = [P10_DVT],
)
const val KEEP_ALIVE_CHANGE_ID = 354143470L

@ChangeId(
  bugId = 390390338L,
  owner = "scottjonathan",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  available = "2025-02-24",
  tags = [P10_DVT],
)
const val DEFAULT_INTERRUPT_SUPPORT = 390390338L

@ChangeId(
  bugId = 394763768L,
  owner = "iancheng",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  available = "2025-04-08",
)
const val SEND_LARGE_DATA_IN_CHUNKS_CHANGE_ID = 394763768L

@ChangeId(
  bugId = 409902959L,
  owner = "iancheng",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  available = "2025-08-02",
  tags = [P11_EVT, NEXT],
)
const val SETUP_SESSION_BASED_STORAGE_CHANGE_ID = 409902959L

/**
 * Feature flag that enables storing task states in disk.
 *
 * This enables system health improvements, by allowing the binder on the server app to be released
 * without losing states. This also enables us to implement `runOrResumeTask` OnboardingTaskManager
 * APIs. In general, this enables us to cope better with process deaths and device reboots.
 *
 * This depends on the [SETUP_SESSION_BASED_STORAGE_CHANGE_ID] flag.
 */
@ChangeId(
  bugId = 416177493L,
  owner = "iancheng",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  available = "2025-08-03",
  tags = [P11_EVT, NEXT],
)
const val STORE_TASK_STATES_CHANGE_ID = 416177493L

/**
 * Feature flag that enables implementing the `OnboardingTaskManager.getTaskState` API with a
 * callback.
 *
 * This allows us to perform blocking actions, e.g. restoring task states from disk, before
 * returning the task states via a listener.
 */
@ChangeId(
  bugId = 427841662L,
  owner = "iancheng",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = [P11_EVT, NEXT],
  available = "2025-06-27",
)
const val GET_TASK_STATE_WITH_CALLBACK_ID = 427841662L

/**
 * Feature flag that enables closing service connection after a OnboardingTaskManager RPC is done.
 *
 * This improves system health by releasing the service connection, and thus the corresponding
 * binder object, when it's feasible to do so. There shouldn't be any behavioral changes from the
 * clients' perspective.
 *
 * This depends on the [SETUP_SESSION_BASED_STORAGE_CHANGE_ID], [STORE_TASK_STATES_CHANGE_ID] and
 * [GET_TASK_STATE_WITH_CALLBACK_ID] flags. Since we can only release the service connection with
 * task states persistence in place.
 */
@ChangeId(
  bugId = 428071871L,
  owner = "iancheng",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  available = "2025-08-04",
  tags = [P11_EVT, NEXT],
)
const val CLOSE_SERVICE_CONNECTION_AFTER_RPC_ID = 428071871L

/**
 * Feature flag that enables fully asynchronous implementation of the
 * [com.android.onboarding.tasks.IOnboardingTaskManagerService].
 *
 * If this change is enabled, the server will report
 * [com.android.onboarding.tasks.IOnboardingTaskManagerService.version] as `1` (which indicates that
 * new callback-based APIs are available for the clients). Additionally, all legacy API calls will
 * be delegated to the [com.android.onboarding.tasks.service.OnboardingTaskServiceServer], which
 * already handles all the calls for the new APIs. With the flag enabled,
 * [com.android.onboarding.tasks.IOnboardingTaskManagerService] essentially becomes a stateless IPC
 * bridge, only responsible for routing IPC calls to
 * [com.android.onboarding.tasks.service.OnboardingTaskServiceServer].
 *
 * @see CHANGE_ID_ASYNC_TASK_CLIENT
 */
@ChangeId(
  bugId = 429537734L,
  owner = "petuska",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = [P11_EVT, NEXT],
  available = "2025-08-18",
)
const val CHANGE_ID_ASYNC_TASK_SERVER = 429537734L

/**
 * Feature flag that enables usage of the
 * [com.android.onboarding.tasks.service.OnboardingTaskServiceClient].
 *
 * If this change is enabled and [CHANGE_ID_ASYNC_TASK_SERVER] is enabled for target component,
 * [com.android.onboarding.tasks.service.OnboardingTaskServiceClient] will be used to handle IPC
 * requests. In all other cases, it will fall back to
 * [com.android.onboarding.tasks.service.OnboardingTaskServiceClient.Compat], which approximates the
 * new APIs with legacy AIDL calls.
 *
 * @see CHANGE_ID_ASYNC_TASK_SERVER
 */
@ChangeId(
  bugId = 428685469L,
  owner = "petuska",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = [P11_DVT, NEXT],
  available = "2025-09-09",
)
const val CHANGE_ID_ASYNC_TASK_CLIENT = 428685469L

/**
 * Feature flag that enables local binder for same-component
 * [com.android.onboarding.tasks.OnboardingTaskManager] execution within all task managers.
 */
@ChangeId(
  bugId = 441696383L,
  owner = "petuska",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = [P11_EVT, NEXT],
  available = "2025-09-18",
)
const val CHANGE_ID_FULL_LOCAL_BINDER = 441696383L
