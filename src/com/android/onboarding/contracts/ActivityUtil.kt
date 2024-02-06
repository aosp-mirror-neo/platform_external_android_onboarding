package com.android.onboarding.contracts

import android.app.Activity
import com.android.onboarding.nodes.AndroidOnboardingGraphLog
import com.android.onboarding.nodes.OnboardingGraphLog.OnboardingEvent.ActivityNodeFail

/** Mark the executing node as failed with [reason]. */
fun Activity.failNode(reason: String? = null) {
  AndroidOnboardingGraphLog.log(ActivityNodeFail(nodeId, reason))
}
