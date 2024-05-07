package com.android.onboarding.contracts

import androidx.activity.result.ActivityResult

/** A result returned by an onboarding node. */
sealed interface NodeResult {
  interface Success : NodeResult

  interface Failure : NodeResult
}

/* A result returned by an [Activity] node. */
interface ActivityNodeResult {
  fun toActivityResult(): ActivityResult
}
