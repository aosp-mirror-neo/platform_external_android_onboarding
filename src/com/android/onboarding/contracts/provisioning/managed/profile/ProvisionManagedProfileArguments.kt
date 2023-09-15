package com.android.onboarding.contracts.provisioning.managed.profile

import android.content.pm.ActivityInfo
import android.net.Uri
import com.android.onboarding.contracts.provisioning.suw.SuwArguments

/**  */
interface ProvisionManagedProfileArguments : SuwArguments {
  val trigger: Int
  val deviceAdminComponentName: ActivityInfo
  val deviceAdminSignatureChecksum: String
  val deviceAdminPackageDownloadLocation: Uri
}
