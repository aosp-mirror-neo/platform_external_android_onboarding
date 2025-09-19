package com.android.onboarding.contracts

import android.os.Bundle
import com.android.onboarding.nodes.NodeId
import com.google.errorprone.annotations.CanIgnoreReturnValue

/** A wrapper around a request to launch an activity contract without depending on the result. */
interface ActivityLauncher<I> {

  /**
   * Launches this contract with the given input.
   *
   * If specified, the flags will be applied to the intent, replacing any already present. If
   * specified, the options will be used for the [startActivity] call.
   */
  @CanIgnoreReturnValue fun launch(input: I, flags: Int? = null, options: Bundle? = null): NodeId

  @CanIgnoreReturnValue fun launch(input: I) = launch(input, flags = null, options = null)

  @CanIgnoreReturnValue
  fun launch(input: I, flags: Int) = launch(input, flags = flags, options = null)

  @CanIgnoreReturnValue
  fun launch(input: I, options: Bundle) = launch(input, flags = null, options = options)
}

@CanIgnoreReturnValue
fun ActivityLauncher<Unit>.launch(flags: Int? = null, options: Bundle? = null) =
  launch(Unit, flags, options)

@CanIgnoreReturnValue fun ActivityLauncher<Unit>.launch() = launch(Unit)
