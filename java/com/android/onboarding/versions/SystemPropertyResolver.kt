package com.android.onboarding.versions

/**
 * A resolver for System properties which check for both, `name` and `persist.name` flavours.
 *
 * Default behaviour is to check `name` before `persist.name`, but it can be inverted by setting
 * [KEY_PREFER_PERSIST_PROPERTIES] system property to true before any other properties are read
 * through this wrapper.
 *
 * @property getter A getter for raw system property values. Usually will be
 *   [android.os.SystemProperties.get] or [java.lang.System.getProperty]. This should ensure to
 *   return null when a property value is deemed invalid to enable other flavour checking.
 * @param preferPersistProperties Should `persist.name` be checked before `name` flavour. Null by
 *   default which will enable lazy resolution of [KEY_PREFER_PERSIST_PROPERTIES] property for the
 *   boolean value. If neither is set it defaults to false.
 * @see AndroidSystemPropertyResolver
 */
abstract class SystemPropertyResolver<T>(
  private val getter: (key: String) -> T?,
  preferPersistProperties: Lazy<Boolean>,
) {
  private val preferPersistProperties by preferPersistProperties

  /** Get the system property for [key]. */
  operator fun get(key: String): T? =
    if (preferPersistProperties) {
      getter("persist.${key}") ?: getter(key)
    } else {
      getter(key) ?: getter("persist.${key}")
    }

  companion object {
    /**
     * System property key to invert the default property check order to prefer `persist.name`
     * flavour.
     */
    const val KEY_PREFER_PERSIST_PROPERTIES = "com.android.onboarding.preferPersistProperties"
  }
}
