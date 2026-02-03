package com.android.onboarding.tasks

import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef
import java.util.function.Supplier

/**
 * A container class to construct ond manage all relevant providers based on passed in data.
 *
 * A "provider" in onboarding context is anything that is exposing data through [ProviderContract]
 * without relying on specific classpath states.
 */
@OptIn(InternalOnboardingApi::class)
object ProviderManager {
  /**
   * @param component The [ComponentRef] this manager is operating under.
   * @param activities All [OnboardingActivityApiContracts][OnboardingActivityApiContract] exposed
   *   via providers.
   * @param tasks All [OnboardingTaskContracts][OnboardingTaskContract] exposed via providers.
   * @param owner The owning object managing these providers. Most of the time it will be the
   *   [OnboardingTaskManagerServiceBinder] instance.
   */
  fun buildProviders(
    component: ComponentRef,
    activities: Set<OnboardingActivityApiContract<*, *>>,
    tasks: Set<OnboardingTaskContract<*, *>>,
    owner: Any,
  ): Map<NodeRef.Stable, Supplier<out ProviderTask<*, *>>> =
    mapOf(
        ContractMetadataProviderContract to
          Supplier { ContractMetadataProviderTask(component, activities, tasks) },
        PendingContractProviderContract to
          Supplier { PendingContractProviderTask(component, activities, tasks) },
        DiagnosisProviderContract to
          Supplier { DiagnosisProviderTask(component, System.identityHashCode(owner)) },
      )
      .mapKeys { (k, _) -> k.build(component).asNodeRef() }
}
