package com.android.onboarding.encoding

/** Provides [Encoder] to use to convert inputs from [Typed] to [Raw]. */
interface WithInputEncoder<in Typed, out Raw> {
  /** The [Encoder] to use to convert inputs from [Typed] to [Raw]. */
  val inputEncoder: Encoder<Typed, Raw>
}

/** Provides [Encoder] to use to convert inputs from [Raw] to [Typed]. */
interface WithInputDecoder<in Raw, out Typed> {
  /** The [Encoder] to use to convert inputs from [Raw] to [Typed]. */
  val inputDecoder: Encoder<Raw, Typed>
}

/** A handler able to convert inputs from [Raw] to [Typed] and back. */
interface InputHandler<Raw, Typed> : WithInputEncoder<Typed, Raw>, WithInputDecoder<Raw, Typed>
