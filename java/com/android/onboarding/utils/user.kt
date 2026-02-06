package com.android.onboarding.utils

import android.os.Process
import android.os.UserHandle
import androidx.annotation.RequiresApi

/** Internal constant to match [android.os.UserHandle.PER_USER_RANGE]. */
@PublishedApi internal const val PER_USER_RANGE = 100_000

/**
 * Returns current user identifier.
 *
 * It is equivalent to [Process.myUserHandle.identifier], but instead of using hidden system APIs it
 * calculates the identifier from [Process.myUid].
 *
 * Namely, it uses the same formula as
 * [android.os.UserHandle.getUserId](https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/os/UserHandle.java;drc=61197364367c9e404c7da6900658f1b16c42d0da;l=286?q=UserHandle.java):
 * [Process.myUid] / [android.os.UserHandle.PER_USER_RANGE]
 */
inline val currentUserIdentifier: Int
  get() = Process.myUid() / PER_USER_RANGE

/**
 * Constructs a [UserHandle] for [userIdentifier].
 *
 * It is equivalent to [UserHandle.of], but instead of using hidden system APIs it calls
 * [UserHandle.getUserHandleForUid].
 *
 * UID is calculated using the formula: [userIdentifier] * [UserHandle.PER_USER_RANGE]
 */
@RequiresApi(24)
fun userHandleOf(userIdentifier: Int): UserHandle =
  UserHandle.getUserHandleForUid(userIdentifier * PER_USER_RANGE)
