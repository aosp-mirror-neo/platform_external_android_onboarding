package com.android.onboarding.tasks.crossApp

import android.content.Context
import com.android.onboarding.tasks.OnboardingTaskContract
import com.android.onboarding.tasks.OnboardingTaskStateManager
import com.android.onboarding.tasks.OnboardingTaskToken

/**
 * Manages the execution of cross-process tasks.
 *
 * This class facilitates the execution of tasks that need to run in a separate process. It provides
 * methods for running tasks and query the tasks.
 */
class CrossProcessTaskManager
private constructor(
  private val appContext: Context,
  private val taskStateManager: OnboardingTaskStateManager,
) {

  fun <
    TaskArgsT,
    TaskResultT,
    TaskContractT : OnboardingTaskContract<TaskArgsT, TaskResultT>,
  > runTask(contract: TaskContractT, args: TaskArgsT): OnboardingTaskToken {
    val onboardingTaskServiceItem = OnboardingTaskServiceItem(appContext, taskStateManager)

    // Trigger cross process task.
    val taskToken =
      onboardingTaskServiceItem.runTask(contract as OnboardingTaskContract<Any?, Any?>, args)

    return taskToken
  }

  companion object {
    @Volatile // Mark as volatile for thread safety
    private var instance: CrossProcessTaskManager? = null

    fun getInstance(
      context: Context,
      taskStateManager: OnboardingTaskStateManager,
    ): CrossProcessTaskManager {
      return instance
        ?: synchronized(this) {
          instance ?: CrossProcessTaskManager(context, taskStateManager).also { instance = it }
        }
    }
  }
}
