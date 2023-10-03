package com.android.onboarding.contracts.provisioning.managed.profile

import android.content.Intent
import android.net.Uri
import com.android.onboarding.contracts.IntentSerializer
import com.android.onboarding.contracts.provisioning.EXTRAS
import com.android.onboarding.common.IntentWriter
import com.android.onboarding.contracts.setupwizard.SuwArgumentsSerializer

class ProvisionManagedProfileArgumentsSerializer(
  private val suwArgumentsSerializer: SuwArgumentsSerializer = SuwArgumentsSerializer(),
) : IntentSerializer<ProvisionManagedProfileArguments> {

  override fun write(intent: Intent, value: ProvisionManagedProfileArguments): Unit =
    with(IntentWriter) {
      suwArgumentsSerializer.write(intent, value.suwArguments)
      intent["trigger"] = value.trigger
      intent[EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME] = value.deviceAdminComponentName
      intent[EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM] =
        value.deviceAdminSignatureChecksum
      intent[EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION] =
        value.deviceAdminPackageDownloadLocation
    }

  override fun read(intent: Intent): ProvisionManagedProfileArguments =
    with(IntentWriter) {
      ProvisionManagedProfileArguments(
        suwArguments = suwArgumentsSerializer.read(intent),
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
}
