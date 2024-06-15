package com.android.onboarding.bedsteadonboarding.fakes

import android.content.ContentValues
import android.content.Context
import android.os.Build
import com.android.onboarding.bedsteadonboarding.contractutils.ContractUtils
import com.android.onboarding.bedsteadonboarding.providers.ConfigProviderUtil
import com.android.onboarding.contracts.ContractResult
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlin.reflect.KClass

/** Represents a fake activity node. */
class FakeActivityNode<I, O>(
  val appsStoringFakeNodeConfig: Set<String>,
  val activityNode: KClass<out OnboardingActivityApiContract<I, O>>,
  val context: Context,
) {

  /**
   * Stores the [ActivityResult] to return when the activity node identified by [this] is faked and
   * is attempted to be executed for result.
   *
   * @param contractResult The [ActivityResult] to return
   */
  @CanIgnoreReturnValue
  fun returnsResult(contractResult: ContractResult): FakeActivityNode<I, O> {
    val bundle =
      when (contractResult) {
        is ContractResult.Success,
        is ContractResult.Failure ->
          FakeActivityNodeHelper.createBundleFromContractResultAndIdentifier(
            contractResult,
            ContractUtils.getContractIdentifier(activityNode.java),
          )
        else ->
          error(
            "Unsupported Contract Result type $contractResult for fake node ${activityNode.simpleName}"
          )
      }

    // Since passing of bundle to [ContentResolver#insert] is supported from API level 30.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      error("Faking response of activity node is not supported for Android API level < 30")
    }

    // Insert bundle to all packages storing test configurations.
    for (appStoringConfig in appsStoringFakeNodeConfig) {
      val uri =
        ConfigProviderUtil.getFakeActivityNodeConfigInsertionUri(packageName = appStoringConfig)
      context.contentResolver.insert(uri, ContentValues(), bundle)
    }

    return this
  }
}
