package com.android.onboarding.contracts.authmanaged

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.android.onboarding.common.AUTH_MANAGED
import com.android.onboarding.contracts.ContractResult
import com.android.onboarding.contracts.IntentScope
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.ScopedIntentSerializer
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.contracts.setupwizard.SuwArguments
import com.android.onboarding.contracts.setupwizard.SuwArgumentsSerializer
import com.android.onboarding.contracts.setupwizard.WithSuwArguments
import javax.inject.Inject

// LINT.IfChange
const val UNMANAGED_WORK_PROFILE_MODE_UNSPECIFIED: Int = 0

// LINT.ThenChange(//depot/google3/java/com/google/android/gmscore/integ/libs/common_auth/src/com/google/android/gms/common/auth/ui/ManagedAccountUtil.java)

data class EmmArguments(
  override val suwArguments: SuwArguments,
  val account: Account,
  val options: Bundle = Bundle.EMPTY,
  val flow: Int? = null,
  val dmStatus: String? = null,
  val isSetupWizard: Boolean = false,
  val suppressDeviceManagement: Boolean = false,
  val callingPackage: String = "",
  val isMainUser: Boolean = false,
  val isUnicornAccount: Boolean = false,
  val unmanagedWorkProfileMode: Int = UNMANAGED_WORK_PROFILE_MODE_UNSPECIFIED,
) : WithSuwArguments

sealed interface EmmResult {
  val code: Int

  sealed interface Success : EmmResult {
    data object OK : Success {
      override val code = Activity.RESULT_OK
    }

    data class Unknown(override val code: Int) : Success
  }

  sealed interface Failure : EmmResult {
    data object LaunchAppFailed : Failure {
      override val code = RESULTS.RESULT_LAUNCH_APP_FAILED
    }

    data object FetchAppInfoFailed : Failure {
      override val code = RESULTS.RESULT_FETCH_APP_INFO_FAILED
    }

    data object DownloadInstallFailed : Failure {
      override val code = RESULTS.RESULT_DOWNLOAD_INSTALL_FAILED
    }

    data object DmNotSupported : Failure {
      override val code = RESULTS.RESULT_DM_NOT_SUPPORTED
    }

    data object DownloadInstallCanceled : Failure {
      override val code = RESULTS.RESULT_DOWNLOAD_INSTALL_CANCELED
    }

    data object ShouldSkipDm : Failure {
      override val code = RESULTS.RESULT_SHOULD_SKIP_DM
    }

    data object SetupSuppressedByCaller : Failure {
      override val code = RESULTS.RESULT_SETUP_SUPPRESSED_BY_CALLER
    }

    data object DisallowAddingManagedAccount : Failure {
      override val code = RESULTS.RESULT_DISALLOW_ADDING_MANAGED_ACCOUNT
    }

    data object CancelledForceRemoveAccount : Failure {
      override val code = RESULTS.RESULT_CANCELLED_FORCE_REMOVE_ACCOUNT
    }

    data object Cancelled : Failure {
      override val code = Activity.RESULT_CANCELED
    }

    data class Unknown(override val code: Int) : Failure
  }
}

/**
 * Contract for {@link EmmChimeraActivity}. Note that the component of this contract intent is
 * assumed to be set by the caller if launching via Component
 */
@OnboardingNode(
  component = AUTH_MANAGED,
  name = "Emm",
  hasUi = OnboardingNode.HasUi.YES_TEMPORARILY,
)
@RequiresApi(Build.VERSION_CODES.Q)
class EmmContract @Inject constructor(val suwArgumentsSerializer: SuwArgumentsSerializer) :
  OnboardingActivityApiContract<EmmArguments, EmmResult>(), ScopedIntentSerializer<EmmArguments> {

  override fun performCreateIntent(context: Context, arg: EmmArguments): Intent = Intent(arg = arg)

  override fun performExtractArgument(intent: Intent): EmmArguments = read(intent)

  override fun performParseResult(result: ContractResult): EmmResult =
    when {
      result.resultCode == RESULTS.RESULT_LAUNCH_APP_FAILED -> EmmResult.Failure.LaunchAppFailed
      result.resultCode == RESULTS.RESULT_FETCH_APP_INFO_FAILED ->
        EmmResult.Failure.FetchAppInfoFailed
      result.resultCode == RESULTS.RESULT_DOWNLOAD_INSTALL_FAILED ->
        EmmResult.Failure.DownloadInstallFailed
      result.resultCode == RESULTS.RESULT_DM_NOT_SUPPORTED -> EmmResult.Failure.DmNotSupported
      result.resultCode == RESULTS.RESULT_DOWNLOAD_INSTALL_CANCELED ->
        EmmResult.Failure.DownloadInstallCanceled
      result.resultCode == RESULTS.RESULT_SHOULD_SKIP_DM -> EmmResult.Failure.ShouldSkipDm
      result.resultCode == RESULTS.RESULT_SETUP_SUPPRESSED_BY_CALLER ->
        EmmResult.Failure.SetupSuppressedByCaller
      result.resultCode == RESULTS.RESULT_DISALLOW_ADDING_MANAGED_ACCOUNT ->
        EmmResult.Failure.DisallowAddingManagedAccount
      result.resultCode == RESULTS.RESULT_CANCELLED_FORCE_REMOVE_ACCOUNT ->
        EmmResult.Failure.CancelledForceRemoveAccount
      result.resultCode == Activity.RESULT_CANCELED -> EmmResult.Failure.Cancelled
      result.resultCode == Activity.RESULT_OK -> EmmResult.Success.OK
      result.resultCode > 0 -> EmmResult.Success.Unknown(result.resultCode)
      else -> EmmResult.Failure.Unknown(result.resultCode)
    }

  override fun performSetResult(result: EmmResult): ContractResult =
    if (result is EmmResult.Success) {
      ContractResult.Success(result.code)
    } else {
      ContractResult.Failure(result.code)
    }

  override fun IntentScope.write(value: EmmArguments) {
    write(suwArgumentsSerializer, value.suwArguments)
    intent[EXTRAS.EXTRA_ACCOUNT] = value.account
    intent[EXTRAS.EXTRA_IS_SETUP_WIZARD] = value.isSetupWizard
    intent[EXTRAS.EXTRA_SUPPRESS_DEVICE_MANAGEMENT] = value.suppressDeviceManagement
    intent[EXTRAS.EXTRA_CALLING_PACKAGE] = value.callingPackage
    intent[EXTRAS.EXTRA_IS_USER_OWNER] = value.isMainUser
    intent[EXTRAS.EXTRA_DM_STATUS] = value.dmStatus
    intent[EXTRAS.EXTRA_IS_UNICORN_ACCOUNT] = value.isUnicornAccount
    intent[EXTRAS.EXTRA_FLOW] = value.flow
    intent[EXTRAS.EXTRA_OPTIONS] = value.options
    intent[EXTRAS.EXTRA_UNMANAGED_WORK_PROFILE_MODE] = value.unmanagedWorkProfileMode
  }

  override fun IntentScope.read(): EmmArguments =
    EmmArguments(
      suwArguments = read(suwArgumentsSerializer),
      account = parcelableExtra(EXTRAS.EXTRA_ACCOUNT),
      options = parcelableExtra(EXTRAS.EXTRA_OPTIONS) ?: Bundle.EMPTY,
      flow = intExtraOrNull(EXTRAS.EXTRA_FLOW),
      dmStatus = stringExtraOrNull(EXTRAS.EXTRA_DM_STATUS),
      isSetupWizard = booleanExtraOrNull(EXTRAS.EXTRA_IS_SETUP_WIZARD) ?: false,
      suppressDeviceManagement =
        booleanExtraOrNull(EXTRAS.EXTRA_SUPPRESS_DEVICE_MANAGEMENT) ?: false,
      callingPackage = stringExtraOrNull(EXTRAS.EXTRA_CALLING_PACKAGE) ?: "",
      isMainUser = booleanExtraOrNull(EXTRAS.EXTRA_IS_USER_OWNER) ?: false,
      isUnicornAccount = booleanExtraOrNull(EXTRAS.EXTRA_IS_UNICORN_ACCOUNT) ?: false,
      unmanagedWorkProfileMode =
        intExtraOrNull(EXTRAS.EXTRA_UNMANAGED_WORK_PROFILE_MODE)
          ?: UNMANAGED_WORK_PROFILE_MODE_UNSPECIFIED,
    )
}
