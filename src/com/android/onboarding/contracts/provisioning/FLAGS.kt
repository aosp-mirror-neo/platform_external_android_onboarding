package com.android.onboarding.contracts.provisioning

import android.app.admin.DevicePolicyManager

object FLAGS {
  inline val FLAG_SUPPORTED_MODES_ORGANIZATION_OWNED: Int
    get() = DevicePolicyManager.FLAG_SUPPORTED_MODES_ORGANIZATION_OWNED

  inline val FLAG_SUPPORTED_MODES_DEVICE_OWNER: Int
    get() = DevicePolicyManager.FLAG_SUPPORTED_MODES_DEVICE_OWNER
}
