package com.android.onboarding.contracts

import android.app.Activity
import android.content.Intent
import com.android.onboarding.nodes.AndroidOnboardingGraphLog
import com.android.onboarding.nodes.EXTRA_ONBOARDING_NODE_ID
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeFail
import java.util.UUID

val Intent.nodeId: Long
  get() =
    getLongExtra(EXTRA_ONBOARDING_NODE_ID, OnboardingActivityApiContract.UNKNOWN_NODE).let {
      if (it == OnboardingActivityApiContract.UNKNOWN_NODE) {
        val id = UUID.randomUUID().leastSignificantBits
        this.putExtra(EXTRA_ONBOARDING_NODE_ID, id)
        id
      } else {
        it
      }
    }

val Activity.nodeId: Long
  get() {
    intent = intent ?: Intent()
    return intent.nodeId
  }

/** Mark the executing node as failed with [reason]. */
fun Activity.failNode(reason: String? = null) {
  AndroidOnboardingGraphLog.log(ActivityNodeFail(nodeId, this.javaClass, reason))
}
