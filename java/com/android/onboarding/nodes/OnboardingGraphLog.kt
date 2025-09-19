package com.android.onboarding.nodes

import java.time.Instant

/**
 * A target for onboarding logs which can register observers to store those logs.
 *
 * Note that this will only include part of the overall graph, and must be linked with the graph
 * from other onboarding components to see a complete view.
 */
interface OnboardingGraphLog {

  /** Add an observer for future graph changes. */
  fun addObserver(observer: Observer)

  /** Remove the observer so it stops receiving updates. */
  fun removeObserver(observer: Observer)

  /**
   * Log an Onboarding event.
   *
   * This will be propagated to all observers.
   */
  fun log(event: OnboardingEvent)

  /** Interface to implement to observe [OnboardingEvent] instances. */
  fun interface Observer {
    /** Called when an [OnboardingEvent] occurs. */
    fun onEvent(event: OnboardingEvent)
  }

  /** Common data applicable to all onboarding events. */
  interface OnboardingEventData : Comparable<Instant>, UniqueNodeRef {
    val timestamp: Instant

    override fun compareTo(other: Instant): Int = timestamp.compareTo(other)

    operator fun compareTo(other: OnboardingEventData): Int = timestamp.compareTo(other.timestamp)
  }

  /** A wrapper over an actual onboarding event. */
  interface OnboardingEventDelegate : OnboardingEventData {
    /** The actual [OnboardingEvent] this delegates to. */
    val source: OnboardingEvent

    /** Compares this delegate with [other] delegate for [source] property equality. */
    infix fun eq(other: OnboardingEventDelegate): Boolean = source == other.source

    override val nodeName: String
      get() = source.nodeName

    override val nodeComponent: String
      get() = source.nodeComponent

    override val nodeId: Long
      get() = source.nodeId

    override val timestamp: Instant
      get() = source.timestamp
  }

  /**
   * Default onboarding graph log.
   *
   * @see AndroidOnboardingGraphLog
   */
  companion object : OnboardingGraphLog {
    private val delegate: OnboardingGraphLog = PlatformOnboardingGraphLog

    override fun addObserver(observer: Observer) = delegate.addObserver(observer)

    override fun removeObserver(observer: Observer) = delegate.removeObserver(observer)

    override fun log(event: OnboardingEvent) = delegate.log(event)
  }
}
