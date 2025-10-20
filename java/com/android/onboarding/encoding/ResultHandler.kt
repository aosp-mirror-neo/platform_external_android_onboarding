package com.android.onboarding.encoding

/** Provides [Encoder] to use to convert results from [Raw] to [Typed]. */
interface WithResultDecoder<in Raw, out Typed> {
  /** The [Encoder] to use to convert results from [Raw] to [Typed]. */
  val resultDecoder: Encoder<Raw, Typed>
}

/** Provides [Encoder] to use to convert results from [Typed] to [Raw]. */
interface WithResultEncoder<out Raw, in Typed> {
  /** The [Encoder] to use to convert results from [Typed] to [Raw]. */
  val resultEncoder: Encoder<Typed, Raw>
}

/** A handler able to convert results from [Raw] to [Typed] and back. */
interface ResultHandler<Raw, Typed> : WithResultDecoder<Raw, Typed>, WithResultEncoder<Raw, Typed>
