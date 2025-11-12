package com.android.onboarding.contracts.provisioning

import android.os.PersistableBundle
import com.android.onboarding.metadata.EncodedMetadata
import com.android.onboarding.metadata.MetadataWriter
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.NullSafePersistableBundleExtensions.getPersistableOrThrow
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.putPersistable

/**
 * Data class that contains OEM customizations for the provisioning flow.
 *
 * @property customUserConsentDetails details about the custom user consent screen.
 */
data class OemProvisioningCustomizations(val customUserConsentDetails: CustomUserConsentDetails) :
  Persistable, StableEquality, MetadataWriter {
  override fun toPersistableBundle() =
    PersistableBundle().apply {
      putPersistable(KEY_CUSTOM_USER_CONSENT_DETAILS, customUserConsentDetails)
    }

  override fun toNonSensitiveMetadata(): List<EncodedMetadata> =
    customUserConsentDetails.toNonSensitiveMetadata()

  companion object : PersistableDeserializer<OemProvisioningCustomizations> {
    internal const val KEY_CUSTOM_USER_CONSENT_DETAILS = "customUserConsentDetails"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      OemProvisioningCustomizations(
        persistableBundle.getPersistableOrThrow(
          KEY_CUSTOM_USER_CONSENT_DETAILS,
          CustomUserConsentDetails,
        )
      )
  }
}
