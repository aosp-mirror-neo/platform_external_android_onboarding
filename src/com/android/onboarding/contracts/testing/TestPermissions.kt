package com.android.onboarding.contracts.testing

// DO NOT MERGE WITH THIS SET TO TRUE.
const val ALLOW_DEBUGGING = false

object TestPermissions {

  /**
   * Returns true if the calling uid is permitted to utilise test-only functionality in Onboarding.
   *
   * This will only return true in situations where it is safe for the caller to use test-only
   * functionality. Additional security checks (such as for rooted devices) should not be performed
   * if this returns true.
   *
   * It is not possible to query in-general if we are running within a test. The standard pattern is
   * to expose some test-only capability (guarded with this method) which sets state that can be
   * queried later. For example, exposing a test-only API to override an allow-list - rather than
   * having the production code skip the allowlist when running in a test.
   */
  fun canCallerExecuteTestFunctionality(uid: Long): Boolean = ALLOW_DEBUGGING
}
