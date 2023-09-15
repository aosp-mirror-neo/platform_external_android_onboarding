package com.android.onboarding.contracts.provisioning.managed.profile

import android.content.Context
import android.content.Intent
import com.android.onboarding.contracts.VoidOnboardingActivityApiContract
import com.android.onboarding.contracts.provisioning.ACTIONS

/**
 * | Activity | com.android.managedprovisioning.preprovisioning.PreProvisioningActivity |
 * |----------|-------------------------------------------------------------------------|
 * |          |                                                                         |
 */
class ProvisionManagedProfileContract :
  VoidOnboardingActivityApiContract<ProvisionManagedProfileArguments>() {
  private val serializer = ProvisionManagedProfileArgumentsSerializer()

  override fun performCreateIntent(
    context: Context,
    arg: ProvisionManagedProfileArguments
  ): Intent = Intent(ACTIONS.ACTION_PROVISION_MANAGED_PROFILE).also { serializer.write(it, arg) }

  override fun performExtractArgument(intent: Intent): ProvisionManagedProfileArguments =
    serializer.read(intent)
}
