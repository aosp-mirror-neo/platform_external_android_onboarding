package com.android.onboarding.tasks.security

import android.content.Context
import android.os.Binder
import android.os.Process

/** A class that ensures tasks are started and accessed securely. */
abstract class TaskPermissionManager {

  /**
   * Returns if a caller is authorized to run or access tasks.
   *
   * The caller is represented by [callerPackageNames] - see
   * [android.content.pm.PackageManager.getPackagesForUid] for more details.
   */
  abstract fun isCallerAuthorized(context: Context, callerPackageNames: Set<String>): Boolean

  /**
   * Enforce task caller permission according to [isCallerAuthorized].
   *
   * Throws [SecurityException] if the caller is unauthorized to run or access tasks.
   */
  @JvmOverloads
  open fun enforceCallerPermission(context: Context, callerUid: Int = Binder.getCallingUid()) {
    if (callerUid == Process.myUid()) {
      return
    }
    val packages = context.packageManager.getPackagesForUid(callerUid)
    if (isCallerAuthorized(context, packages?.toSet() ?: setOf())) {
      return
    }
    throw SecurityException(
      "Caller is unauthorized to call ${context.packageName} to run or access tasks"
    )
  }
}
