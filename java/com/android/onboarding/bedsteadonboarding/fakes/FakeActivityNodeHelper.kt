package com.android.onboarding.bedsteadonboarding.fakes

import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.android.onboarding.contracts.ContractResult

/** Contains helper methods for [FakeActivityNode] */
object FakeActivityNodeHelper {

  private const val FAKE_CONTRACT_CLASS_IDENTIFIER = "FAKE_CONTRACT_CLASS_IDENTIFIER"
  private const val CONTRACT_RESULT_INTENT = "CONTRACT_RESULT_INTENT"
  private const val CONTRACT_RESULT_CODE = "CONTRACT_RESULT_CODE"
  private const val CONTRACT_RESULT_TYPE = "CONTRACT_RESULT_TYPE"
  private const val CONTRACT_RESULT_FAILURE_REASON = "CONTRACT_RESULT_FAILURE_REASON"
  private const val CONTRACT_RESULT_TYPE_SUCCESS = "CONTRACT_RESULT_SUCCESS"
  private const val CONTRACT_RESULT_TYPE_FAILURE = "CONTRACT_RESULT_FAILURE"

  /** Serializes the given [contractResult] and [contractIdentifier] to bundle. */
  fun createBundleFromContractResultAndIdentifier(
    contractResult: ContractResult,
    contractIdentifier: String,
  ): Bundle {
    return Bundle().apply {
      putParcelable(CONTRACT_RESULT_INTENT, contractResult.intent)
      putInt(CONTRACT_RESULT_CODE, contractResult.resultCode)
      putString(FAKE_CONTRACT_CLASS_IDENTIFIER, contractIdentifier)
      putString(CONTRACT_RESULT_TYPE, getContractResultType(contractResult))
      if ((contractResult is ContractResult.Failure) && (contractResult.reason != null)) {
        putString(CONTRACT_RESULT_FAILURE_REASON, contractResult.reason)
      }
    }
  }

  /**
   * Extracts and returns the [contractResult] and [contractIdentifier] from given [bundle]. This
   * will return null when the bundle does not contain [contractIdentifier] information.
   */
  fun extractContractIdentifierAndContractResultFromBundle(
    bundle: Bundle
  ): Pair<String, ContractResult>? {
    val contractClassIdentifier = bundle.getString(FAKE_CONTRACT_CLASS_IDENTIFIER) ?: return null

    return Pair(contractClassIdentifier, createContractResultFromBundle(bundle))
  }

  private fun getContractResultType(contractResult: ContractResult): String {
    return when (contractResult) {
      is ContractResult.Success -> CONTRACT_RESULT_TYPE_SUCCESS
      is ContractResult.Failure -> CONTRACT_RESULT_TYPE_FAILURE
      else -> error("Unsupported contractResult type $contractResult")
    }
  }

  private fun createContractResultFromBundle(bundle: Bundle): ContractResult {
    val resultCode = bundle.getInt(CONTRACT_RESULT_CODE)
    val responseIntent =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        bundle.getParcelable(CONTRACT_RESULT_INTENT, Intent::class.java)
      } else {
        bundle.getParcelable(CONTRACT_RESULT_INTENT)
      }

    val contractResultType =
      bundle.getString(CONTRACT_RESULT_TYPE)
        ?: error("ContractResult type shouldn't be null in bundle $bundle")
    return when (contractResultType) {
      CONTRACT_RESULT_TYPE_SUCCESS -> ContractResult.Success(resultCode, responseIntent)
      CONTRACT_RESULT_TYPE_FAILURE -> {
        val failureReason = bundle.getString(CONTRACT_RESULT_FAILURE_REASON)
        ContractResult.Failure(resultCode, responseIntent, failureReason)
      }
      else -> error("Invalid contract result type $contractResultType")
    }
  }
}
