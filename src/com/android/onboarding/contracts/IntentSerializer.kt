package com.android.onboarding.contracts

import android.content.Intent

/** Interface for writing an object to an [Intent] and reading the same object from an [Intent]. */
interface IntentSerializer<V> {
  fun write(intent: Intent, value: V)

  fun read(intent: Intent): V
}
