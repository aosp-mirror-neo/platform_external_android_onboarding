package com.android.onboarding.contracts.provisioning

import android.os.PersistableBundle
import com.android.onboarding.metadata.EncodedMetadata
import com.android.onboarding.metadata.EncodedMetadata.BooleanMetadata
import com.android.onboarding.metadata.EncodedMetadata.EnumMetadata
import com.android.onboarding.metadata.MetadataWriter
import com.android.onboarding.metadata.NonSensitiveString
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.logging.OnboardingLog
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableEnum
import com.android.onboarding.utils.persistable.PersistableIntent
import com.android.onboarding.utils.persistable.PersistableSerializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable

/**
 * Details about an OEM-provided custom user consent screen that will be shown during provisioning.
 */
sealed class CustomUserConsentDetails : Persistable, StableEquality, MetadataWriter {

  override fun toPersistableBundle(): PersistableBundle = Companion.toPersistableBundle(this)

  /** Represents the case where no custom user consent screen is required. */
  data object CustomUserConsentNotRequired : CustomUserConsentDetails() {
    override fun toNonSensitiveMetadata(): List<EncodedMetadata> =
      listOf(BooleanMetadata(NonSensitiveString(CUSTOM_USER_CONSENT_REQUIRED_METADATA_NAME), false))
  }

  /**
   * Represents the case where a custom user consent screen is required.
   *
   * @property customUserConsentIntent the intent to launch the custom user consent screen.
   * @property customUserConsentPosition where in the provisioning flow the custom user consent
   *   screen should be shown.
   */
  data class CustomUserConsentRequired(
    val customUserConsentIntent: PersistableIntent,
    val customUserConsentPosition: CustomUserConsentPosition,
  ) : CustomUserConsentDetails() {
    override fun toNonSensitiveMetadata(): List<EncodedMetadata> =
      listOf(
        BooleanMetadata(NonSensitiveString(CUSTOM_USER_CONSENT_REQUIRED_METADATA_NAME), true),
        EnumMetadata(
          NonSensitiveString(CUSTOM_USER_CONSENT_POSITION_METADATA_NAME),
          customUserConsentPosition,
        ),
      )

    internal companion object {
      const val KEY_CUSTOM_USER_CONSENT_INTENT = "customUserConsentIntent"
      const val KEY_CUSTOM_USER_CONSENT_POSITION = "customUserConsentPosition"
    }
  }

  /**
   * Enum representing where in the provisioning flow the custom user consent screen should be
   * shown.
   */
  enum class CustomUserConsentPosition(override val id: String) : PersistableEnum, StableEquality {
    /** The custom user consent screen should be shown before the role holder consent screen. */
    BEFORE_ROLE_HOLDER_CONSENT_SCREEN("BEFORE_ROLE_HOLDER_CONSENT_SCREEN"),
    /**
     * The custom user consent screen should be shown inside the role holder "view terms" screen.
     */
    INSIDE_ROLE_HOLDER_VIEW_TERMS("INSIDE_ROLE_HOLDER_VIEW_TERMS");

    companion object :
      PersistableDeserializer<CustomUserConsentPosition> by PersistableEnum.deserializer(entries)
  }

  companion object :
    PersistableDeserializer<CustomUserConsentDetails>,
    PersistableSerializer<CustomUserConsentDetails> {
    private val log = OnboardingLog.logger("CustomUserConsentDetails")
    internal const val KEY_TYPE = "type"
    internal const val TYPE_NOT_REQUIRED = "notRequired"
    internal const val TYPE_REQUIRED = "required"
    internal const val CUSTOM_USER_CONSENT_REQUIRED_METADATA_NAME = "customUserConsentRequired"
    internal const val CUSTOM_USER_CONSENT_POSITION_METADATA_NAME = "customUserConsentPosition"

    override fun toPersistableBundle(value: CustomUserConsentDetails): PersistableBundle =
      when (value) {
        is CustomUserConsentNotRequired ->
          PersistableBundle().apply { putString(KEY_TYPE, TYPE_NOT_REQUIRED) }
        is CustomUserConsentRequired ->
          PersistableBundle().apply {
            putString(KEY_TYPE, TYPE_REQUIRED)
            putPersistable(
              CustomUserConsentRequired.KEY_CUSTOM_USER_CONSENT_INTENT,
              value.customUserConsentIntent,
            )
            putPersistable(
              CustomUserConsentRequired.KEY_CUSTOM_USER_CONSENT_POSITION,
              value.customUserConsentPosition,
            )
          }
      }

    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): CustomUserConsentDetails {
      return when (val type = persistableBundle.getString(KEY_TYPE)) {
        TYPE_NOT_REQUIRED -> CustomUserConsentNotRequired
        TYPE_REQUIRED -> extractConsentDetailsForRequiredType(persistableBundle)
        else -> {
          log.e("Unknown CustomUserConsentDetails type: $type")
          CustomUserConsentNotRequired
        }
      }
    }

    // Attempts to extract [CustomUserConsentRequired] from the persistable bundle, falls
    // back to [CustomUserConsentNotRequired] if the bundle is malformed.
    private fun extractConsentDetailsForRequiredType(
      persistableBundle: PersistableBundle
    ): CustomUserConsentDetails {
      val intent =
        persistableBundle.getPersistable(
          CustomUserConsentRequired.KEY_CUSTOM_USER_CONSENT_INTENT,
          PersistableIntent,
        )
      val position =
        persistableBundle.getPersistable(
          CustomUserConsentRequired.KEY_CUSTOM_USER_CONSENT_POSITION,
          CustomUserConsentPosition,
        )
      return if (intent == null || position == null) {
        log.e("intent or position missing for custom user consent")
        CustomUserConsentNotRequired
      } else {
        CustomUserConsentRequired(intent, position)
      }
    }
  }
}
