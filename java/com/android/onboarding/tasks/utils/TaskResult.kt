package com.android.onboarding.tasks.utils

import com.android.onboarding.metadata.IString
import com.android.onboarding.metadata.NonSensitiveString

/** Class representing task result. */
sealed class TaskResult<V> {

  /** The result of executing a task successfully. */
  data class Success<V>(val result: V) : TaskResult<V>()

  /** The result of a task failure, and the reason for that failure. */
  data class Failure<V>
  @JvmOverloads
  constructor(
    val throwable: Throwable? = null,
    val failureReason: IString = NonSensitiveString("undefined"),
  ) : TaskResult<V>()
}
