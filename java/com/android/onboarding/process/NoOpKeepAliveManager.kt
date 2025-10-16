package com.android.onboarding.process

import android.content.Context
import java.util.function.Supplier

/** A no-op implementation of [KeepAliveManager]. */
class NoOpKeepAliveManager(contextProvider: Supplier<Context>) :
  KeepAliveManager(
    contextProvider = contextProvider,
    controllerPackageName = "noop",
    serviceClassName = "noop",
  ) {
  override fun keepAlive(reason: KeepAliveReason): AutoCloseable = AutoCloseable {}
}
