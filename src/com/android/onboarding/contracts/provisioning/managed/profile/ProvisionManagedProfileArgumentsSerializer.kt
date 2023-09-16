package com.android.onboarding.contracts.provisioning.managed.profile

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import com.android.onboarding.contracts.IntentSerializer
import com.android.onboarding.contracts.provisioning.ArgumentSerializer
import com.android.onboarding.contracts.provisioning.EXTRAS
import com.android.onboarding.contracts.provisioning.suw.SuwArguments
import com.android.onboarding.contracts.provisioning.suw.SuwArgumentsSerializer

class ProvisionManagedProfileArgumentsSerializer(
  private val suwArgumentsSerializer: SuwArgumentsSerializer = SuwArgumentsSerializer(),
) : ArgumentSerializer(), IntentSerializer<ProvisionManagedProfileArguments> {

  override fun write(intent: Intent, value: ProvisionManagedProfileArguments): Unit =
    with(value) {
      suwArgumentsSerializer.write(intent, value)
      intent["trigger"] = trigger
      intent[EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME] = deviceAdminComponentName
      intent[EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM] =
        deviceAdminSignatureChecksum
      intent[EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION] =
        deviceAdminPackageDownloadLocation
    }

  override fun read(intent: Intent): ProvisionManagedProfileArguments {
    return object :
      ProvisionManagedProfileArguments, SuwArguments by suwArgumentsSerializer.read(intent) {

      override val trigger: Int = intent.intExtra(EXTRAS.EXTRA_PROVISIONING_TRIGGER)

      override val deviceAdminComponentName: ActivityInfo =
        intent.parcelableExtra(EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME)

      override val deviceAdminSignatureChecksum: String =
        intent.stringExtra(EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM)

      override val deviceAdminPackageDownloadLocation: Uri =
        intent
          .stringExtra(EXTRAS.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION)
          .let(Uri::parse)
    }
  }
}
