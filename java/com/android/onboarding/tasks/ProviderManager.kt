package com.android.onboarding.tasks

import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.nodes.ComponentRef

/** A container class to construct ond manage all relevant providers based on passed in data. */
class ProviderManager
private constructor(component: ComponentRef, providers: Set<ProviderTask<*, *>>) :
  Set<ProviderTask<*, *>> by providers, ComponentRef by component {
  /**
   * @param component The [ComponentRef] this manager is operating under.
   * @param activities All [OnboardingActivityApiContracts][OnboardingActivityApiContract] exposed
   *   via providers.
   * @param tasks All [OnboardingTaskContracts][OnboardingTaskContract] exposed via providers.
   */
  constructor(
    component: ComponentRef,
    activities: Set<OnboardingActivityApiContract<*, *>>,
    tasks: Set<OnboardingTaskContract<*, *>>,
  ) : this(
    component = component,
    providers =
      setOf(
        ContractMetadataProviderTask(component, activities, tasks),
        PendingContractProviderTask(component, activities, tasks),
      ),
  )
}
