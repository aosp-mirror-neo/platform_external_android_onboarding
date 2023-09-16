package com.android.onboarding.contracts

import android.content.Intent

/** The result of encoding a contract result for passing as an activity result. */
sealed interface ContractResult {
  val resultCode: Int
  val intent: Intent?

  data class Success(
    override val resultCode: Int,
    override val intent: Intent? = null) : ContractResult

  data class Failure(
    override val resultCode: Int,
    override val intent: Intent? = null,
    val reason: String? = null) : ContractResult
}

internal data class UnknownContractResult(
  override val resultCode: Int,
  override val intent: Intent? = null,
) : ContractResult