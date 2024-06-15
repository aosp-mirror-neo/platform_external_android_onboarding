package com.android.onboarding.bedsteadonboarding.contractutils

import android.content.Context
import android.util.Log
import com.android.onboarding.bedsteadonboarding.fakes.FakeActivityNodeHelper
import com.android.onboarding.bedsteadonboarding.permissions.TestPermissions
import com.android.onboarding.bedsteadonboarding.providers.ConfigProviderUtil
import com.android.onboarding.contracts.ContractResult
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.nodes.OnboardingGraphNode

/** Contains utility methods for node contracts. */
object ContractUtils {

  private const val contractIdentifierSeparator = "."
  private const val TAG = "ContractUtils"

  private val isRunningOnDebuggableDevice: Boolean by lazy {
    TestPermissions.isRunningOnDebuggableDevice()
  }

  /** Returns a string uniquely identifying a node's contract given its [contractClass]. */
  fun getContractIdentifier(contractClass: Class<*>) =
    "${OnboardingNode.extractComponentNameFromClass(contractClass)}${contractIdentifierSeparator}${
      OnboardingNode.extractNodeNameFromClass(contractClass)
    }"

  /** Returns a string uniquely identifying the contract for the given [node]. */
  fun getContractIdentifierForNode(node: OnboardingGraphNode) =
    "${node.component}$contractIdentifierSeparator${node.name}"

  /**
   * Returns a string uniquely identifying a node's contract given its [nodeComponent] and
   * [nodeName].
   */
  fun getContractIdentifier(nodeComponent: String, nodeName: String) =
    "${nodeComponent}$contractIdentifierSeparator${nodeName}"

  fun getContractResultIfNodeIsFakedInTest(context: Context, contractClass: Class<*>) =
    getContractResultIfNodeIsFakedInTest(context, getContractIdentifier(contractClass))

  /**
   * Returns the [ContractResult] configured in test if the node identified by [contractIdentifier]
   * is faked. This will always return null when the node is being executed in production. It will
   * also return null when running in test environment if the node is not faked.
   */
  fun getContractResultIfNodeIsFakedInTest(
    context: Context,
    contractIdentifier: String,
  ): ContractResult? {
    try {
      if (isRunningOnDebuggableDevice) {
        return getContractResultForContractIdentifier(context, contractIdentifier)
      }
      return null
    } catch (t: Throwable) {
      // For safety, ignore the exception since current function would run in production flow.
      Log.e(TAG, "Error while fetching fake contract response of contract $contractIdentifier", t)
      return null
    }
  }

  /**
   * Returns the [ContractResult] configured in test if the node identified by [contractIdentifier]
   * is faked. This will always return null when the node is being executed in production. It will
   * also return null when running in test environment if the node is not faked.
   */
  fun getContractResultForContractIdentifier(
    context: Context,
    contractIdentifier: String,
  ): ContractResult? {
    try {
      val uri = ConfigProviderUtil.getFakeActivityNodeConfigQueryUri(context, contractIdentifier)

      val cursor =
        context.contentResolver.query(
          uri,
          arrayOf(),
          /* selection= */ null,
          /* selectionArgs= */ null,
          /* sortOrder= */ null,
          /* cancellationSignal= */ null,
        ) ?: return null

      cursor.use {
        if (it.extras != null) {
          val contractResult =
            FakeActivityNodeHelper.extractContractIdentifierAndContractResultFromBundle(it.extras)
          if (contractResult != null) return contractResult.second
        }
      }
      return null
    } catch (t: Throwable) {
      // For safety, ignore the exception since current function would run in production flow.
      Log.e(TAG, "Error while fetching fake contract response of node $contractIdentifier", t)
      return null
    }
  }
}
