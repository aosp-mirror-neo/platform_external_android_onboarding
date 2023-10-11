package com.android.onboarding.contracts.provisioning

import android.content.Context
import android.content.Intent
import com.android.onboarding.common.MANAGED_PROVISIONING
import com.android.onboarding.contracts.ContractResult
import com.android.onboarding.contracts.IntentScope
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.ScopedIntentSerializer
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.contracts.setupwizard.SuwArguments
import com.android.onboarding.contracts.setupwizard.SuwArgumentsSerializer
import com.android.onboarding.contracts.setupwizard.WithOptionalSuwArguments
import javax.inject.Inject

data class FinalizationInsideSuwArguments(
  override val suwArguments: SuwArguments?,
) : WithOptionalSuwArguments

/**
 * Result is propagated from the child activities the activity described by this contract launches.
 *
 * The activity uses [ProvisioningParams], however they are fetched from the FS rather than intent
 * extras.
 */
@OnboardingNode(
  component = MANAGED_PROVISIONING,
  name = "FinalizationInsideSuw",
  hasUi = OnboardingNode.HasUi.YES
)
class FinalizationInsideSuwContract
@Inject
constructor(val suwArgumentsSerializer: SuwArgumentsSerializer) :
  OnboardingActivityApiContract<FinalizationInsideSuwArguments, ContractResult>(),
  ScopedIntentSerializer<FinalizationInsideSuwArguments> {
  override fun performCreateIntent(context: Context, arg: FinalizationInsideSuwArguments): Intent =
    Intent(ACTIONS.ACTION_ROLE_HOLDER_PROVISION_FINALIZATION).also { write(it, arg) }

  override fun IntentScope.write(value: FinalizationInsideSuwArguments) {
    value.suwArguments?.let { write(suwArgumentsSerializer, it) }
  }

  override fun IntentScope.read(): FinalizationInsideSuwArguments =
    FinalizationInsideSuwArguments(
      suwArguments = read(suwArgumentsSerializer),
    )

  override fun performExtractArgument(intent: Intent): FinalizationInsideSuwArguments = read(intent)

  override fun performParseResult(result: ContractResult): ContractResult = result

  override fun performSetResult(result: ContractResult): ContractResult = result
}
