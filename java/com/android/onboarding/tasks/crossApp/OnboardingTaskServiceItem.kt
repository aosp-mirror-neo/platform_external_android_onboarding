package com.android.onboarding.tasks.crossApp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.android.onboarding.tasks.ERROR_FAILED_BIND_TASK_SERVICE
import com.android.onboarding.tasks.ERROR_RUNNING_TASK
import com.android.onboarding.tasks.OnboardingTaskContract
import com.android.onboarding.tasks.OnboardingTaskState
import com.android.onboarding.tasks.OnboardingTaskStateManager
import com.android.onboarding.tasks.OnboardingTaskToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Represents an item for interacting with an onboarding task service. Provides functionality to
 * bind to the remote service, execute tasks, and query task state etc.
 */
internal class OnboardingTaskServiceItem(
  private val context: Context,
  private val taskStateManager: OnboardingTaskStateManager,
  private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {

  private val serviceBindingChannel = Channel<IOnboardingTaskManagerService>()
  private var taskManagerService: IOnboardingTaskManagerService? = null
  private var isServiceBound = false
  private lateinit var taskToken: OnboardingTaskToken
  private lateinit var bindServiceIntent: Intent

  /**
   * Initiates execution of a task using the bound task manager service.
   *
   * @param bundle The PersistableBundle containing the task data.
   */
  fun runTask(contract: OnboardingTaskContract<Any?, Any?>, args: Any?): OnboardingTaskToken {

    bindServiceIntent = contract.taskServiceIntent
    taskToken =
      OnboardingTaskToken(
        taskContractClass = contract::class.java.name,
        taskComponentName = contract.componentName,
      )

    serviceScope.launch {
      try {
        val service = awaitServiceBinding()
        val persistableBundle = contract.encodeArgs(args)
        service.runTask(persistableBundle)
      } catch (e: Exception) {
        Log.e(TAG, "Error running task: $taskToken", e)
        taskStateManager.updateTaskState(
          taskToken,
          OnboardingTaskState.Failed<Nothing>(ERROR_RUNNING_TASK),
        )
      }
    }

    return taskToken
  }

  /** Unbinds from the onboarding task manager service if currently bound. */
  fun unbindService() {
    if (isServiceBound) {
      context.unbindService(serviceConnection)
      isServiceBound = false
      taskManagerService = null
    }
  }

  private suspend fun awaitServiceBinding(): IOnboardingTaskManagerService {
    if (!isServiceBound) {
      bindService()
      return serviceBindingChannel.receive() // Suspend until service is bound
    }
    // Based on current code implementation, this should be always non-null.
    return taskManagerService!!
  }

  private fun bindService() {
    if (!context.bindService(bindServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)) {
      Log.e(
        TAG,
        "Failed to bind service: ${bindServiceIntent.`package`}/${bindServiceIntent.action}",
      )
      taskStateManager.updateTaskState(
        taskToken,
        OnboardingTaskState.Failed<Nothing>(ERROR_FAILED_BIND_TASK_SERVICE),
      )
    } else {
      Log.i(TAG, "Bound service: ${bindServiceIntent.`package`}/${bindServiceIntent.action}")
    }
  }

  private val serviceConnection =
    object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Log.i(TAG, "Service connected: $name")
        taskManagerService = IOnboardingTaskManagerService.Stub.asInterface(service)
        isServiceBound = true
        serviceScope.launch { serviceBindingChannel.send(taskManagerService!!) }
      }

      override fun onServiceDisconnected(name: ComponentName?) {
        Log.i(TAG, "Service disconnected: $name")
        taskManagerService = null
        isServiceBound = false
      }
    }

  companion object {
    private const val TAG = "OnboardingTaskServiceItem"
  }
}
