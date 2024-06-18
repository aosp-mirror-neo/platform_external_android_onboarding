package com.android.onboarding.bedsteadonboarding.graph

import com.android.bedstead.nene.utils.Poll
import com.android.onboarding.bedsteadonboarding.utils.EventLogUtils.ONBOARDING_EVENT_LOG_TAG
import com.android.onboarding.bedsteadonboarding.logcat.LogcatReader
import com.android.onboarding.nodes.OnboardingGraph
import com.android.onboarding.nodes.OnboardingEvent
import java.time.Duration

/**
 * Entry point to onboarding graph. Used to query onboarding events.
 */
class OnboardingGraphProvider(logcatReader: LogcatReader) {

  internal val graph: OnboardingGraph = OnboardingGraph(getOnboardingEvents(logcatReader))

  /**
   * Get a [OnboardingGraphQueryBuilder] to iteratively build a query to search for events in the
   * onboarding graph.
   */
  fun query(): OnboardingGraphQueryBuilder {
    return OnboardingGraphQueryBuilder(this)
  }

  /** Returns the immutable set of OnboardingEvents read from logs */
  private fun getOnboardingEvents(logcatReader: LogcatReader) =
    Poll.forValue("Onboarding events") { fetchOnboardingEventsFromLogs(logcatReader) }
      .toMeet { it.isNotEmpty() }
      .timeout(Duration.ofMinutes(LOG_READ_TIMEOUT_MINUTE))
      .errorOnFail("No onboarding event logged after $LOG_READ_TIMEOUT_MINUTE minute(s).")
      .await()

  private fun fetchOnboardingEventsFromLogs(logcatReader: LogcatReader) : Set<OnboardingEvent> {
    val onboardingEvents = mutableSetOf<OnboardingEvent>()
    for (logLines in logcatReader.getFilteredLogs()) {
      // Is an onboarding graph line containing OnboardingEvent.
      val encoded = logLines.split("$ONBOARDING_EVENT_LOG_TAG: ")[1]

      // Deserialize and store the OnboardingEvent in-memory.
      val event = OnboardingEvent.deserialize(encoded)
      onboardingEvents.add(event)
    }
    return onboardingEvents.toSet()
  }

  companion object {
    const val LOG_READ_TIMEOUT_MINUTE = 1L
  }
}
