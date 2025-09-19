package com.android.onboarding.compatibility

import android.content.Intent

/** @see Intent.getParcelableExtra */
inline fun <reified T : Any> Intent.compatGetParcelableExtra(key: String): T? =
  compatGetParcelableExtra(key, T::class.java)

/** @see Intent.getParcelableExtra */
fun <T : Any> Intent.compatGetParcelableExtra(key: String, klass: Class<T>): T? =
  if (android.os.Build.VERSION.SDK_INT >= 33) {
    getParcelableExtra(key, klass)
  } else {
    @Suppress("DEPRECATION") getParcelableExtra(key)
  }
