package com.android.onboarding.nodes.testing

import com.android.onboarding.nodes.OnboardingEvent
import com.android.onboarding.nodes.OnboardingGraphLog

class TestOnboardingGraphObserver : OnboardingGraphLog.Observer {

  val events = mutableListOf<OnboardingEvent>()

  override fun onEvent(event: OnboardingEvent) {
    events.add(event)
  }
}
