package com.android.onboarding.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow

/** Collect flow results in the given [scope]. */
suspend fun <T> Flow<T>.collectIn(scope: CoroutineScope): List<T> {
  val collected = mutableListOf<T>()
  scope.async { collect { metadata -> collected.add(metadata) } }.await()
  return collected
}
