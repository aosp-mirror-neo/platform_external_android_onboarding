package com.android.onboarding.contracts.provisioning

import android.app.admin.DevicePolicyManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableEnum

enum class ProvisioningMode(override val id: String) : PersistableEnum {
  FullyManagedDevice("FULLY_MANAGED_DEVICE"),
  ManagedProfile("MANAGED_PROFILE"),
  ManagedProfileOnPersonalDevice("MANAGED_PROFILE_ON_PERSONAL_DEVICE");

  @RequiresApi(Build.VERSION_CODES.Q)
  fun toDevicePolicyManagedId(): Int =
    when (this) {
      FullyManagedDevice -> DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
      ManagedProfile -> DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
      ManagedProfileOnPersonalDevice ->
        DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE_ON_PERSONAL_DEVICE
    }

  companion object :
    PersistableDeserializer<ProvisioningMode> by PersistableEnum.deserializer(entries) {

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
