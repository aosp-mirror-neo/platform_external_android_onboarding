package com.android.onboarding.tasks.crossApp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import android.os.PersistableBundle
import com.android.onboarding.tasks.OnboardingTaskContract
import com.android.onboarding.tasks.OnboardingTaskManager
import com.android.onboarding.tasks.OnboardingTaskToken

/**
 * [AbstractOnboardingTaskManagerService] is a base class for services that manage onboarding tasks.
 * Subclasses of [AbstractOnboardingTaskManagerService] must implement the [taskManager] property to
 * provide an implementation of AbstractOnboardingTaskManager.
 *
 * This service allows clients to interact with the onboarding task manager by binding to it and
 * invoking tasks.
 */
abstract class AbstractOnboardingTaskManagerService : Service() {

  protected abstract val taskManager: OnboardingTaskManager

  override fun onBind(intent: Intent): IBinder {
    return object : IOnboardingTaskManagerService.Stub() {

      override fun runTask(bundle: PersistableBundle): OnboardingTaskToken {
        val contractClass = getContractClass(bundle) ?: return OnboardingTaskToken.INVALID
        val contractInstance =
          OnboardingTaskContract.tryCreateContractInstance(contractClass)
            ?: return OnboardingTaskToken.INVALID
        val contractArgument = contractInstance.extractArgs(bundle)

        return taskManager.runTask(contractInstance, contractArgument)
      }
    }
  }

  /**
   * Parses the contract class name from the given [PersistableBundle]. This function extracts the
   * contract class name from the provided bundle and attempts to find the corresponding contract
   * class in the task manager's mapping. If found, it returns the class; otherwise, it returns
   * null.
   */
  private fun getContractClass(
    bundle: PersistableBundle
  ): Class<OnboardingTaskContract<Any?, Parcelable?>>? {
    val contractClassName = bundle.getString(OnboardingTaskContract.EXTRA_CONTRACT_CLASS)
    val matchingContractClass =
      taskManager
        .getContractAndTaskMap()
        .filterKeys { it.name == contractClassName }
        .keys
        .firstOrNull()
    if (matchingContractClass != null) {
      @Suppress("UNCHECKED_CAST")
      // Because we don't send the input/output types via IPC.
      return matchingContractClass as Class<OnboardingTaskContract<Any?, Parcelable?>>
    } else {
      throw IllegalArgumentException("Contract class not found: $contractClassName")
    }
  }

  companion object {
    private const val TAG: String = "OTMService"
  }
}
