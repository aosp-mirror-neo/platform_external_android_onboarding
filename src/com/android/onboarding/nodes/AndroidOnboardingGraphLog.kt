package com.android.onboarding.nodes

/** A Singleton [OnboardingGraphLog] which automatically logs all events to logcat. */
object AndroidOnboardingGraphLog : OnboardingGraphLog {

  private val logcatObserver = OnboardingGraphLogcatObserver()
  private val defaultOnboardingGraphLog =
    DefaultOnboardingGraphLog().also { it.addObserver(logcatObserver) }

  override fun addObserver(observer: OnboardingGraphLog.Observer) {
    defaultOnboardingGraphLog.addObserver(observer)
  }

  override fun log(event: OnboardingGraphLog.OnboardingEvent) {
    defaultOnboardingGraphLog.log(event)
  }
}
