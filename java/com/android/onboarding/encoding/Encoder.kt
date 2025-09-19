package com.android.onboarding.encoding

/** Interface for encoding from [I] type to [O] type */
fun interface Encoder<in I, out O> {
  /**
   * Encodes an [I] input to an [O] type.
   *
   * @param input The input to encode.
   * @return The encoded [O] type.
   */
  fun encode(input: I): O
}
