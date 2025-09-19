package com.android.onboarding.utils

import android.os.BaseBundle
import unsupported

infix fun BaseBundle?.contentDeepEquals(other: Any?): Boolean =
  contentDeepEquals(other, includeUnderscoredKeys = false)

fun BaseBundle?.contentDeepEquals(other: Any?, includeUnderscoredKeys: Boolean): Boolean =
  unsupported

fun BaseBundle?.generateHash(includeUnderscoredKeys: Boolean = false): Int = unsupported

fun BaseBundle.toStringNoContent(spaces: Boolean = false): String = unsupported
