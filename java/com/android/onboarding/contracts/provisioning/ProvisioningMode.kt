package com.android.onboarding.contracts.provisioning

import android.app.admin.DevicePolicyManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.onboarding.metadata.EncodedMetadata
import com.android.onboarding.metadata.EncodedMetadata.EnumMetadata
import com.android.onboarding.metadata.MetadataWriter
import com.android.onboarding.metadata.NonSensitiveString
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableEnum

/**
 * Represents different device management modes for provisioning.
 *
 * @property id The unique identifier of this enum entry, used for persistence.
 */
enum class ProvisioningMode(override val id: String) :
  PersistableEnum, StableEquality, MetadataWriter {
  FullyManagedDevice("FULLY_MANAGED_DEVICE"),
  ManagedProfile("MANAGED_PROFILE"),
  ManagedProfileOnPersonalDevice("MANAGED_PROFILE_ON_PERSONAL_DEVICE");

  override fun toNonSensitiveMetadata(): List<EncodedMetadata> =
    listOf(EnumMetadata(NonSensitiveString(PROVISIONING_MODE_METADATA_NAME), this))

  @RequiresApi(Build.VERSION_CODES.S)
  fun toDevicePolicyManagedId(): Int =
    when (this) {
      FullyManagedDevice -> DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
      ManagedProfile -> DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
      ManagedProfileOnPersonalDevice ->
        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE
    }

  companion object :
    PersistableDeserializer<ProvisioningMode> by PersistableEnum.deserializer(entries) {
    internal const val PROVISIONING_MODE_METADATA_NAME = "provisioningMode"

    @RequiresApi(Build.VERSION_CODES.Q)
    operator fun invoke(devicePolicyManagedId: Int): ProvisioningMode =
      when (devicePolicyManagedId) {
        DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE -> FullyManagedDevice
        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE -> ManagedProfile
        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE ->
          ManagedProfileOnPersonalDevice
        else -> error("Unknown ProvisioningMode(id=$devicePolicyManagedId)")
      }
  }
}
