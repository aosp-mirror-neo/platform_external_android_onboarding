package com.android.onboarding.versions

import android.os.Build
import android.os.SystemProperties

/** Android-specific [SystemPropertyResolver] backed by [SystemProperties] */
class AndroidSystemPropertyResolver<T>(getter: (key: String) -> T?) :
  SystemPropertyResolver<T>(getter, preferPersistProperties) {

  companion object {
    private val preferPersistProperties = lazy {
      getProp("persist.${KEY_PREFER_PERSIST_PROPERTIES}")?.toBooleanStrictOrNull()
        ?: getProp(KEY_PREFER_PERSIST_PROPERTIES)?.toBooleanStrictOrNull()
        ?: false
    }

    private fun getProp(key: String): String? {
      return if (Build.VERSION.SDK_INT >= 28) {
        SystemProperties.get(key).takeIf(String::isNotBlank)
      } else {
        // On less than 28 we had length limits for system properties - we'll just not support
        // this feature on older versions.
        null
      }
    }
  }
}
