package com.android.onboarding.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.time.toKotlinDuration

/** Copy of [com.google.common.time.TimeSource] API for onboarding use. */
fun interface TimeSource {
  /** Returns the current [Instant] according to this time source. */
  fun instant(): Instant

  companion object {
    /** @see com.google.common.time.TimeSource.system */
    @JvmStatic fun system(): TimeSource = TimeSource(Instant::now)
  }
}

/** Get the [LocalTime] of this [Instant] under UTC timezone. */
val Instant.time: LocalTime
  get() = LocalTime.ofInstant(this, ZoneId.of("UTC"))

/** Returns the time component string of this [Instant] under UTC timezone. */
val Instant.timeString: String
  get() = time.format(DateTimeFormatter.ISO_LOCAL_TIME)

/** Formats a given duration into a human-readable string `[-]HH:mm:ss.sss`. */
fun Duration.format(): String =
  toKotlinDuration().toComponents { hours, minutes, seconds, nanoseconds ->
    val negative = hours < 0 || minOf(minutes, seconds, nanoseconds) < 0
    val ms = (nanoseconds / 1e6).toInt().absoluteValue
    val s = seconds.absoluteValue
    val m = minutes.absoluteValue
    val h = hours.absoluteValue

    val chunks = mutableListOf<String>()
    if (h > 0) chunks.add("${h}h")
    if (m > 0 || h > 0) chunks.add("${m.toString().padStart(2)}min")
    chunks.add("${s.toString().padStart(2)}.${ms.toString().padStart(3, '0')}s")
    (if (negative) "-" else "") + chunks.joinToString(separator = " ").trim()
  }

/** Returns the [Duration] between [start] and `this` [Instant]. */
operator fun Instant.minus(start: Instant): Duration = Duration.between(start, this)

inline val Number.seconds: Duration
  get() = Duration.ofSeconds(toLong())
inline val Number.minutes: Duration
  get() = Duration.ofMinutes(toLong())
inline val Number.hours: Duration
  get() = Duration.ofHours(toLong())
