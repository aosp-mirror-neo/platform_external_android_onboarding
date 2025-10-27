package com.android.onboarding.tasks.security

import android.content.Context

/** A no-op implementation of [TaskPermissionManager] that allows any app to run or access tasks. */
class NoOpTaskPermissionManager : TaskPermissionManager() {
  override fun isCallerAuthorized(context: Context, callerPackageNames: Set<String>) = true
}
