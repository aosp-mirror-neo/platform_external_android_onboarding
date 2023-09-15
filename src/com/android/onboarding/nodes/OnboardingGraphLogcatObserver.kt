package com.android.onboarding.nodes

import android.util.Log

/** An [OnboardingGraphLog.Observer] which serializes all logs into logcat. */
class OnboardingGraphLogcatObserver : OnboardingGraphLog.Observer {

  override fun onEvent(event: OnboardingGraphLog.OnboardingEvent) {
    Log.i(LOG_TAG, event.serializeToString())
  }

  companion object {
    const val LOG_TAG = "ZZZOnboardingGraph"
  }
}
