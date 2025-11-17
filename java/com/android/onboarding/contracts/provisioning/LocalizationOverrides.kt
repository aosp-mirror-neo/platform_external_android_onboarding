package com.android.onboarding.contracts.provisioning

import android.os.PersistableBundle
import com.android.onboarding.metadata.EncodedMetadata
import com.android.onboarding.metadata.EncodedMetadata.BooleanMetadata
import com.android.onboarding.metadata.MetadataWriter
import com.android.onboarding.metadata.NonSensitiveString
import com.android.onboarding.utils.KnownStableEquality
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.NullSafePersistableBundleExtensions.getLongOrNull
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import java.util.IllformedLocaleException
import java.util.Locale

/**
 * Localization values to set on a device after provisioning (applies to all users). Any null
 * property means that that value will not be overridden on the device.
 *
 * @property timeZone an [Olson id](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones)
 *   from the list returned by [java.util.TimeZone.getAvailableIDs].
 * @property localTime the system wall time in milliseconds since the Epoch.
 * @property locale the system locale to set.
 */
data class LocalizationOverrides(
  val timeZone: String? = null,
  val localTime: Long? = null,
  val locale: @KnownStableEquality Locale? = null,
) : Persistable, MetadataWriter, StableEquality {
  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().apply {
      if (timeZone != null) {
        putString(KEY_TIME_ZONE, timeZone)
      }
      if (localTime != null) {
        putLong(KEY_LOCAL_TIME, localTime)
      }
      if (locale != null) {
        putString(KEY_LOCALE, locale.toLanguageTag())
      }
    }

  override fun toNonSensitiveMetadata(): List<EncodedMetadata> =
    listOf(
      BooleanMetadata(NonSensitiveString(METADATA_CONTAINS_TIME_ZONE_OVERRIDE), timeZone != null),
      BooleanMetadata(NonSensitiveString(METADATA_CONTAINS_LOCAL_TIME_OVERRIDE), localTime != null),
      BooleanMetadata(NonSensitiveString(METADATA_CONTAINS_LOCALE_OVERRIDE), locale != null),
    )

  companion object : PersistableDeserializer<LocalizationOverrides> {
    val EMPTY = LocalizationOverrides()

    private const val KEY_TIME_ZONE = "timeZone"
    private const val KEY_LOCAL_TIME = "localTime"
    private const val KEY_LOCALE = "locale"

    internal const val METADATA_CONTAINS_TIME_ZONE_OVERRIDE = "containsTimeZoneOverride"
    internal const val METADATA_CONTAINS_LOCAL_TIME_OVERRIDE = "containsLocalTimeOverride"
    internal const val METADATA_CONTAINS_LOCALE_OVERRIDE = "containsLocaleOverride"

    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): LocalizationOverrides =
      LocalizationOverrides(
        timeZone = persistableBundle.getString(KEY_TIME_ZONE),
        localTime = persistableBundle.getLongOrNull(KEY_LOCAL_TIME),
        locale = stringToLocale(persistableBundle.getString(KEY_LOCALE)),
      )

    /** Converts a String to a Locale. */
    @Throws(IllformedLocaleException::class)
    internal fun stringToLocale(string: String?): Locale? =
      if (string != null) {
        Locale.Builder().setLanguageTag(string.replace("_", "-")).build()
      } else {
        null
      }
  }
}
