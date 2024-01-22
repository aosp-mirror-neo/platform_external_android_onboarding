package com.android.onboarding.bedsteadonboarding.contractutils

import com.android.onboarding.contracts.annotations.OnboardingNode

/** Contains utility methods for node contracts. */
object ContractUtils {

  private const val contractIdentifierSeparator = "."

  /** Returns a string uniquely identifying a node's contract given its [contractClass]. */
  fun getContractIdentifier(contractClass: Class<*>) =
    "${OnboardingNode.extractComponentNameFromClass(contractClass)}${contractIdentifierSeparator}${OnboardingNode.extractNodeNameFromClass(contractClass)}"
}
