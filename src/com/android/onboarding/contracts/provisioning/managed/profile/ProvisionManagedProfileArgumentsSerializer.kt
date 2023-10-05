package com.android.onboarding.contracts.provisioning.managed.profile

import android.net.Uri
import com.android.onboarding.contracts.IntentScope
import com.android.onboarding.contracts.ScopedIntentSerializer
import com.android.onboarding.contracts.provisioning.EXTRAS
import com.android.onboarding.contracts.setupwizard.SuwArgumentsSerializer

class ProvisionManagedProfileArgumentsSerializer(
  private val suwArgumentsSerializer: SuwArgumentsSerializer = SuwArgumentsSerializer(),
) : ScopedIntentSerializer<ProvisionManagedProfileArguments> {

  override fun IntentScope.write(value: ProvisionManagedProfileArguments) {
    intent.write(suwArgumentsSerializer, value.suwArguments)
    intent["trigger"] = value.trigger
    intent[EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME] = value.deviceAdminComponentName
    intent[EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM] =
      value.deviceAdminSignatureChecksum
    intent[EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION] =
      value.deviceAdminPackageDownloadLocation
  }

  override fun IntentScope.read(): ProvisionManagedProfileArguments =
    ProvisionManagedProfileArguments(
      suwArguments = intent.read(suwArgumentsSerializer),
      trigger = intent.intExtra(EXTRAS.EXTRA_PROVISIONING_TRIGGER),
      deviceAdminComponentName =
        intent.parcelableExtra(EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME),
      deviceAdminSignatureChecksum =
        intent.stringExtra(EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM),
      deviceAdminPackageDownloadLocation =
        intent
          .stringExtra(EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION)
          .let(Uri::parse),
    )
}
