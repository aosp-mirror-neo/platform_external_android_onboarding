package com.android.onboarding.contracts.provisioning

import android.content.Context
import android.content.Intent
import com.android.onboarding.contracts.VoidOnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.DoesNotNeedUi

// From DevicePolicyManager
private const val ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE =
  "android.app.action.PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE"

val PARSER = ManagedDeviceProvisioningArgumentsSerializer()

/**
 * Starts the enterprise provisioning flow, either by updating and using the Device Policy Manager
 * Role Holder or by offline provisioning if the role holder is not available or
 * [ManagedDeviceProvisioningArguments.allowOfflineProvisioning] is set to true.
 *
 * <p>During device owner provisioning, a device admin app is downloaded and set as the owner of the
 * device. A device owner has full control over the device. The device owner can not be modified by
 * the user and the only way of resetting the device is via factory reset.
 *
 * <p>If there is no internet connection, and
 * [ManagedDeviceProvisioningArguments.allowOfflineProvisioning] is not set to {code true}, then
 * (TODO: How do we expose the failure?)
 *
 * <p>The DPMRH updater will be launched to allow updating of the Device Policy Manager Role Holder.
 *
 * <p>After interacting with the updater, (TODO: We will pass control to the role holder using X
 * contract)
 *
 * <p>This API should only be called on a device which is not yet provisioned (TODO: Define what
 * this means)
 */
@DoesNotNeedUi
class ProvisionOfflineOrViaRoleHolderApiContract :
  VoidOnboardingActivityApiContract<ManagedDeviceProvisioningArguments>() {
  override fun performCreateIntent(
    context: Context,
    arg: ManagedDeviceProvisioningArguments
  ): Intent {
    val intent = Intent(ACTIONS.ACTION_PROVISION_MANAGED_DEVICE_FROM_TRUSTED_SOURCE)
    PARSER.write(intent, arg)
    return intent
  }

  override fun performExtractArgument(intent: Intent): ManagedDeviceProvisioningArguments =
    PARSER.read(intent)
}
