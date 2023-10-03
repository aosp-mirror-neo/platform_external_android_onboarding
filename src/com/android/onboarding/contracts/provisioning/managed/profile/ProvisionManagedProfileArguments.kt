package com.android.onboarding.contracts.provisioning.managed.profile

import android.content.pm.ActivityInfo
import android.net.Uri
import com.android.onboarding.contracts.setupwizard.SuwArguments
import com.android.onboarding.contracts.setupwizard.WithSuwArguments

/**  */
data class ProvisionManagedProfileArguments(
  override val suwArguments: SuwArguments,
  val trigger: Int,
  val deviceAdminComponentName: ActivityInfo,
  val deviceAdminSignatureChecksum: String,
  val deviceAdminPackageDownloadLocation: Uri,
) : WithSuwArguments
