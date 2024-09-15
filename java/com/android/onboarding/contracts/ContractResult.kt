package com.android.onboarding.contracts

import android.content.Intent
import androidx.activity.result.ActivityResult
import com.android.onboarding.contracts.annotations.InternalOnboardingApi

/** An [ActivityNodeResult] used by lightweight Activity node contracts. */
sealed interface ContractResult : ActivityNodeResult {
  val resultCode: Int
  val intent: Intent?

  override fun toActivityResult() = ActivityResult(resultCode, intent)

  data class Success
  @JvmOverloads
  constructor(override val resultCode: Int, override val intent: Intent? = null) :
    ContractResult, NodeResult.Success

  data class Failure
  @JvmOverloads
  constructor(
    override val resultCode: Int,
    override val intent: Intent? = null,
    val reason: String? = null,
  ) : ContractResult, NodeResult.Failure
}

@InternalOnboardingApi
data class UnknownContractResult(
  override val resultCode: Int,
  override val intent: Intent? = null,
) : ContractResult, NodeResult.Success
