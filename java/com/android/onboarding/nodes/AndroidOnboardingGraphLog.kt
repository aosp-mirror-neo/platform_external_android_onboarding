package com.android.onboarding.nodes

import com.android.onboarding.flags.DefaultOnboardingFlagsProvider

/** A Singleton [OnboardingGraphLog] which automatically logs all events to logcat. */
internal object AndroidOnboardingGraphLog : OnboardingGraphLog {

  const val LOG_TAG = "ZZZOnboardingGraph"
  private const val LOG_TAG_DEBUG = "DDDOnboardingGraph"


  private val onboardingFlags = DefaultOnboardingFlagsProvider()
  private val defaultOnboardingGraphLog =
    DefaultOnboardingGraphLog().apply {
      addObserver(LogcatObserver(LOG_TAG, OnboardingEvent::serializeToString))
      if (onboardingFlags.isDebug) {
        addObserver(LogcatObserver(LOG_TAG_DEBUG, OnboardingEvent::toString))
      }

      if (onboardingFlags.shouldVisualiseNodeTransitionsInLogcat) {
        addObserver(
          OnboardingGraphNodeTransitionObserver(
            LogcatObserver(logTag = "OnboardingGraph") { node ->
              "${node.nodeName}(${node.nodeId})"
            }
          )
        )
      }
    }

  override fun addObserver(observer: OnboardingGraphLog.Observer) {
    defaultOnboardingGraphLog.addObserver(observer)
  }

  override fun removeObserver(observer: OnboardingGraphLog.Observer) {
    defaultOnboardingGraphLog.removeObserver(observer)
  }

  override fun log(event: OnboardingEvent) {
    defaultOnboardingGraphLog.log(event)
  }
}

internal inline val PlatformOnboardingGraphLog: OnboardingGraphLog
  get() = AndroidOnboardingGraphLog
