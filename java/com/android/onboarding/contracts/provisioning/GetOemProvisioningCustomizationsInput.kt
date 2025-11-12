package com.android.onboarding.contracts.provisioning

import android.os.PersistableBundle
import com.android.onboarding.metadata.EncodedMetadata
import com.android.onboarding.metadata.MetadataWriter
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.NullSafePersistableBundleExtensions.getPersistableOrThrow
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.getPersistableList
import com.android.onboarding.utils.persistable.putPersistable
import com.android.onboarding.utils.persistable.putPersistableList
import kotlin.collections.List

/**
 * Input data for [GetOemProvisioningCustomizationsContract].
 *
 * @property provisioningMode the provisioning mode that is going to be set on the device.
 * @property provisioningDisclaimers a list of disclaimers to be shown during provisioning.
 * @property localizationOverrides localization values to set on the device after provisioning.
 */
data class GetOemProvisioningCustomizationsInput(
  val provisioningMode: ProvisioningMode,
  val provisioningDisclaimers: List<ProvisioningDisclaimer> = emptyList(),
  val localizationOverrides: LocalizationOverrides = LocalizationOverrides.EMPTY,
) : Persistable, StableEquality, MetadataWriter {
  override fun toPersistableBundle() =
    PersistableBundle().apply {
      putPersistable(KEY_PROVISIONING_MODE, provisioningMode)
      putPersistableList(KEY_PROVISIONING_DISCLAIMERS, provisioningDisclaimers)
      putPersistable(KEY_LOCALIZATION_OVERRIDES, localizationOverrides)
    }

  override fun toNonSensitiveMetadata(): List<EncodedMetadata> =
    provisioningMode.toNonSensitiveMetadata() + localizationOverrides.toNonSensitiveMetadata()

  companion object : PersistableDeserializer<GetOemProvisioningCustomizationsInput> {
    private const val KEY_PROVISIONING_MODE = "provisioningMode"
    private const val KEY_PROVISIONING_DISCLAIMERS = "provisioningDisclaimers"
    private const val KEY_LOCALIZATION_OVERRIDES = "localizationOverrides"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      GetOemProvisioningCustomizationsInput(
        provisioningMode =
          persistableBundle.getPersistableOrThrow(KEY_PROVISIONING_MODE, ProvisioningMode),
        provisioningDisclaimers =
          persistableBundle
            .getPersistableList(KEY_PROVISIONING_DISCLAIMERS, ProvisioningDisclaimer)
            ?.filterNotNull() ?: emptyList(),
        localizationOverrides =
          persistableBundle.getPersistable(KEY_LOCALIZATION_OVERRIDES, LocalizationOverrides)
            ?: LocalizationOverrides.EMPTY,
      )
  }
}
