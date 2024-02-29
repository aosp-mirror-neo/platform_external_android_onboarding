package com.android.onboarding.common

import com.android.onboarding.common.annotations.OnboardingComponent

@OnboardingComponent(name = "Managed Provisioning", packageName = "com.android.managedprovisioning")
const val MANAGED_PROVISIONING = "com.android.managedprovisioning"
@OnboardingComponent(
  name = "Test App",
  packageName = "com.android.onboarding.nodes.testing.testapp",
)
const val TEST_APP = "com.android.onboarding.nodes.testing.testapp"
@OnboardingComponent(
  name = "Auth Managed",
  packageName = "com.google.android.gms",
  subName = "auth_managed",
  blueprint = "//java/com/google/android/gmscore/integ/modules/auth_managed/auth_managed.blueprint",
)
const val AUTH_MANAGED = "com.google.android.gms/auth_managed"

val COMPONENTS =
  mapOf(
    "MANAGED_PROVISIONING" to MANAGED_PROVISIONING,
    "TEST_APP" to TEST_APP,
    "AUTH_MANAGED" to AUTH_MANAGED,
  )
