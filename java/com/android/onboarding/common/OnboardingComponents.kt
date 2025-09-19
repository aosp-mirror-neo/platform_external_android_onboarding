package com.android.onboarding.common

import com.android.onboarding.common.annotations.OnboardingComponent
import com.android.onboarding.common.annotations.Triage

@OnboardingComponent(
  name = "Managed Provisioning",
  packageName = "com.android.managedprovisioning",
  subName = "managedprovisioning",
  triage =
    Triage(
      buganizerComponentId = 1337923,
      assigneeEmailAddress = "ae-provisioning-triage@google.com",
    ),
)
const val MANAGED_PROVISIONING = "com.android.managedprovisioning/managedprovisioning"

/**
 * This is a special component representing all the "external" components that are part of the
 * onboarding flow, but have not yet integrated with its APIs.
 */
@OnboardingComponent(
  name = "External",
  subName = "external",
  packageName = "com.android.onboarding.external",
  triage = Triage(buganizerComponentId = 1392556, assigneeEmailAddress = "scottjonathan@google.com"),
)
const val EXTERNAL = "com.android.onboarding.external/external"

@OnboardingComponent(
  name = "Test App",
  subName = "testapp",
  packageName = "com.android.onboarding.nodes.testing.testapp",
  triage = Triage(buganizerComponentId = 1392556, assigneeEmailAddress = "scottjonathan@google.com"),
)
const val TEST_APP = "com.android.onboarding.nodes.testing.testapp/testapp"

@OnboardingComponent(
  name = "Test App 2",
  subName = "testapp2",
  packageName = "com.android.onboarding.nodes.testing.testapp2",
  triage = Triage(buganizerComponentId = 1392556, assigneeEmailAddress = "scottjonathan@google.com"),
)
const val TEST_APP_2 = "com.android.onboarding.nodes.testing.testapp2/testapp2"


@OnboardingComponent(
  name = "Auth Managed",
  packageName = "com.google.android.gms",
  subName = "auth_managed",
  blueprint = "//java/com/google/android/gmscore/integ/modules/auth_managed/auth_managed.blueprint",
)
const val AUTH_MANAGED = "com.google.android.gms/auth_managed"


@OnboardingComponent(
  name = "Setup Wizard",
  packageName = "com.google.android.setupwizard",
  subName = "setupwizard",
  blueprint = "//java/com/google/android/apps/setupwizard/setupwizard.blueprint",
)
const val SETUP_WIZARD = "com.google.android.setupwizard/setupwizard"

val COMPONENTS: Map<String, String> =
  mapOf(
    "AUTH_MANAGED" to AUTH_MANAGED,
    "MANAGED_PROVISIONING" to MANAGED_PROVISIONING,
    "SETUP_WIZARD" to SETUP_WIZARD,
    "TEST_APP" to TEST_APP,
  )
