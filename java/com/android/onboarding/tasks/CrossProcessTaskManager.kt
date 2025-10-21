package com.android.onboarding.tasks

import com.android.onboarding.tasks.service.OnboardingTaskService

/**
 * Manages the execution of cross-process tasks.
 *
 * This interface facilitates the execution of tasks that need to run in a separate process. It
 * provides methods for running tasks and query the tasks.
 */
interface CrossProcessTaskManager : OnboardingTaskService, AutoCloseable
