package com.android.onboarding.contracts.provisioning

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.onboarding.metadata.EncodedMetadata
import com.android.onboarding.metadata.EncodedMetadata.EnumMetadata
import com.android.onboarding.metadata.MetadataWriter
import com.android.onboarding.metadata.NonSensitiveString
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.logging.OnboardingLog
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableEnum

/**
 * Represents different device management modes for provisioning.
 *
 * @property id The unique identifier of this enum entry, used for persistence.
 */
enum class ProvisioningMode(override val id: String) :
  PersistableEnum, StableEquality, MetadataWriter {
  // Should never be used directly, this is just for cross-component backwards compatibility
  Unknown("UNKNOWN"),
  FullyManagedDevice("FULLY_MANAGED_DEVICE"),
  ManagedProfile("MANAGED_PROFILE"),
  ManagedProfileOnPersonalDevice("MANAGED_PROFILE_ON_PERSONAL_DEVICE");

  override fun toNonSensitiveMetadata(): List<EncodedMetadata> =
    listOf(EnumMetadata(NonSensitiveString(PROVISIONING_MODE_METADATA_NAME), this))

  /**
   * Returns the provisioning constant in [DevicePolicyManager] that corresponds to this mode, or
   * [Activity.RESULT_CANCELED] if this mode is [Unknown].
   */
  @RequiresApi(Build.VERSION_CODES.S)
  fun toDevicePolicyManagerId(): Int =
    when (this) {
      FullyManagedDevice -> DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
      ManagedProfile -> DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
      ManagedProfileOnPersonalDevice ->
        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE
      Unknown -> {
        log.w("Converting unknown mode to a DevicePolicyManagerId")
        Activity.RESULT_CANCELED
      }
    }

  companion object :
    PersistableDeserializer<ProvisioningMode> by PersistableEnum.deserializer(
      entries,
      default = Unknown,
    ) {
    internal const val PROVISIONING_MODE_METADATA_NAME = "provisioningMode"
    private val log = OnboardingLog.logger("ProvisioningMode")

    /**
     * Returns the provisioning mode that corresponds to [devicePolicyManagerId], or [Unknown] if
     * [devicePolicyManagerId] is not recognised.
     *
     * @param devicePolicyManagerId an integer that corresponds to one of the PROVISIONING_MODE_
     *   constants defined in [DevicePolicyManager].
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    operator fun invoke(devicePolicyManagerId: Int): ProvisioningMode =
      when (devicePolicyManagerId) {
        DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE -> FullyManagedDevice
        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE -> ManagedProfile
        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE ->
          ManagedProfileOnPersonalDevice
        else -> {
          log.w("Unknown ProvisioningMode(id=$devicePolicyManagerId)")
          Unknown
        }
      }
  }
}
