package com.android.onboarding.utils

import com.google.common.hash.HashCode
import com.google.common.hash.Hashing
import java.nio.charset.StandardCharsets

private fun hashString(input: String): HashCode =
  Hashing.farmHashFingerprint64().hashString(input, StandardCharsets.UTF_8)

/** Produces a stable hash of [input] as a hex [String]. */
fun hashStringToHex(input: String): String = hashString(input).toString()
