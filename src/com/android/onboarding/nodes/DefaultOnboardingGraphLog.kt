package com.android.onboarding.nodes

import java.util.Collections
import java.util.WeakHashMap

class DefaultOnboardingGraphLog : OnboardingGraphLog {

  private val observers =
    Collections.newSetFromMap(WeakHashMap<OnboardingGraphLog.Observer, Boolean>())

  override fun addObserver(observer: OnboardingGraphLog.Observer) {
    observers.add(observer)
  }

  override fun log(event: OnboardingGraphLog.OnboardingEvent) {
    observers.forEach { it.onEvent(event) }
  }
}
