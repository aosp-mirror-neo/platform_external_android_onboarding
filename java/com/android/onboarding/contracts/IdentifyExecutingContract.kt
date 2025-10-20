package com.android.onboarding.contracts

import android.content.Intent

/**
 * Interface implemented by a [OnboardingActivityApiContract] to allow it to be used with
 * [findExecutingContract].
 *
 * This allows a single activity to implement multiple contracts, as long as those contracts can be
 * distinguished based on properties of the [Intent].
 *
 * Recommended usage:
 * ```
 * val contracts = arrayOf(RedContract(), BlueContract(), GreenContract())
 * val contract = intent?.findExecutingContract(*contracts) ?: RedContract()
 * ```
 */
interface IdentifyExecutingContract {
  /** Returns true if this [OnboardingActivityApiContract] is currently executing. */
  fun isExecuting(intent: Intent): Boolean
}

/**
 * Find which of multiple non-overlapping contracts are executing.
 *
 * See [IdentifyExecutingContract] for usage.
 */
fun <I : IdentifyExecutingContract> Intent.findExecutingContract(vararg contracts: I) =
  contracts.firstOrNull { it.isExecuting(this) }

/** More lightweight version of [IdentifyExecutingContract] working with intent actions only. */
interface IdentifyExecutingContractByAction : IdentifyExecutingContract {
  /** Returns true if this [OnboardingActivityApiContract] is currently executing. */
  fun isExecuting(action: String?): Boolean

  override fun isExecuting(intent: Intent): Boolean = isExecuting(intent.action)
}

/** @see Intent.findExecutingContract */
fun <I : IdentifyExecutingContractByAction> findExecutingContract(
  action: String?,
  vararg contracts: I,
) = contracts.firstOrNull { it.isExecuting(action) }
