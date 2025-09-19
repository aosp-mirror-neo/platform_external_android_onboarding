package com.android.onboarding.metadata

import com.android.onboarding.utils.StableEquality

/**
 * Wrapper around a string which can be either privacy non-sensitive (as represented by
 * [NonSensitiveString]) or unknown (as represented by [SensitiveString].
 */
interface IString : StableEquality {
  val value: String

  /**
   * Get a [NonSensitiveString] representation of this string.
   *
   * If this string is sensitive, then the value of the [NonSensitiveString] will be set to
   * "Redacted".
   */
  fun toNonSensitiveString() = NonSensitiveString("Redacted")
}

/** Wrapper around a string for which the privacy status is unknown. */
@JvmInline
value class SensitiveString(override val value: String) : IString {
  override fun toString(): String = "SensitiveString($value)"
}
