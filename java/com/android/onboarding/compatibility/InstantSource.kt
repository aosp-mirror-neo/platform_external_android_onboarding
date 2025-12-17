package com.android.onboarding.compatibility

import androidx.annotation.RequiresApi
import java.time.Instant

/** Equivalent to java.time.InstantSource but supported from SDK 26+ */
@RequiresApi(26)
fun interface InstantSource {
  fun instant(): Instant
}

/** Create a backwards-compatible [InstantSource] from a java.time one. */
@RequiresApi(34)
fun java.time.InstantSource.toCompatInstantSource(): InstantSource {
  val t = this
  return object : InstantSource {
    override fun instant(): Instant {
      return t.instant()
    }
  }
}
