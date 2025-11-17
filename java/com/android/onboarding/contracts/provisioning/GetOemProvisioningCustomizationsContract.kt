package com.android.onboarding.contracts.provisioning

import com.android.onboarding.common.MANAGED_PROVISIONING
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.tasks.OnboardingTaskContractWithPersistables

/**
 * Contract for a task that obtains provisioning customizations, such as whether a custom user
 * consent screen needs to be shown during role holder provisioning.
 */
@OnboardingNode(
  component = MANAGED_PROVISIONING,
  name = "GetOemProvisioningCustomizations",
  specificationType = OnboardingNode.SpecificationType.V1,
  timeoutInSeconds = 30L,
)
object GetOemProvisioningCustomizationsContract :
  OnboardingTaskContractWithPersistables<
    GetOemProvisioningCustomizationsInput,
    OemProvisioningCustomizations,
  >() {
  override val argsExtractor = GetOemProvisioningCustomizationsInput
  override val resultExtractor = OemProvisioningCustomizations
}
