package com.android.onboarding.bedsteadonboarding

import android.content.ContentValues
import androidx.test.platform.app.InstrumentationRegistry
import com.android.onboarding.bedsteadonboarding.annotations.TestNodes
import com.android.onboarding.bedsteadonboarding.contractutils.ContractUtils
import com.android.onboarding.bedsteadonboarding.data.NodeData
import com.android.onboarding.bedsteadonboarding.data.OnboardingNodeMetadata
import com.android.onboarding.bedsteadonboarding.providers.ConfigProviderUtil
import com.android.onboarding.bedsteadonboarding.providers.ConfigProviderUtil.TEST_NODE_CLASS_COLUMN
import com.android.onboarding.contracts.annotations.OnboardingNode
import kotlin.reflect.KClass
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
/**
 * TestRule to run before and after each test run. It will store all the test node related configs
 * like which nodes are allowed to execute.
 */
class OnboardingTestsRule : TestRule {

  private val instrumentationContext = InstrumentationRegistry.getInstrumentation().context

  override fun apply(base: Statement, description: Description): Statement {
    return if (description.isTest) {
      applyTest(base, description)
    } else {
      error(
        "OnboardingTestsRule can only be used as a `@Rule`. Unexpected description type: $description"
      )
    }
  }

  private fun applyTest(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        val testNodesConfiguration =
          extractTestNodesAnnotation(description)?.let { processTestNodesAnnotation(it) }
            ?: TestNodesConfiguration()
        try {
          // Create an array of [ContentValues] representing the Test Configs.
          val contentValuesForTestConfigs =
            createContentValuesForAllowedNodes(testNodesConfiguration.allowedNodes)

          /* Now that we have an array of [allowedNodes] present in [contentValuesForTestConfigs],
            store them in all the apps in [appsStoringTestConfig] using their [TestContentProvider].
          */
          for (appPackageName in testNodesConfiguration.appsStoringTestConfig) {
            insertTestConfigsInApp(contentValuesForTestConfigs, appPackageName)
          }

          base.evaluate()
        } finally {
          deleteAllTestConfigs(testNodesConfiguration.appsStoringTestConfig)
        }
      }
    }
  }

  /**
   * Stores the test configs using the ContentProvider of the app with given [appPackageName].
   *
   * @param contentValues An array of ContentValues representing the test configurations.
   * @param appPackageName package name of the app where test configs would be stored.
   */
  private fun insertTestConfigsInApp(contentValues: Array<ContentValues>, appPackageName: String) {
    /* Delete test configurations stored by app before inserting new ones. */
    deleteTestConfigsOfApp(appPackageName)
    val uri =
      ConfigProviderUtil.getTestConfigContentUri(ConfigProviderUtil.getAuthority(appPackageName))
    instrumentationContext.contentResolver.bulkInsert(uri, contentValues)
  }

  /**
   * From the [TestNodes] annotation, extract all the nodes which are allowed to execute and also
   * the package name of the apps containing the nodes and store them in [TestNodesConfiguration].
   */
  private fun processTestNodesAnnotation(testNodes: TestNodes): TestNodesConfiguration {
    val allowedNodes = mutableListOf<NodeData>()
    val appsStoringTestConfig = mutableSetOf<String>(instrumentationContext.packageName)

    for (node in testNodes.nodes) {
      extractAllowedNodeAndItsAppPackage(node.contract).apply {
        allowedNodes.add(first)
        appsStoringTestConfig.add(second)
      }
    }
    return TestNodesConfiguration(appsStoringTestConfig.toSet(), allowedNodes.toList())
  }

  /**
   * Given a node's contract class, returns its [NodeData] representation and the package name of
   * the app to which it belongs.
   */
  private fun extractAllowedNodeAndItsAppPackage(nodeContract: KClass<*>): Pair<NodeData, String> {
    for (annotation in nodeContract.annotations) {
      if (annotation is OnboardingNode) {
        val onboardingNodeMetadata = getOnboardingNodeMetadata(annotation)
        return Pair(
          NodeData(
            allowedContractIdentifier = ContractUtils.getContractIdentifier(nodeContract.java)
          ),
          onboardingNodeMetadata.packageName,
        )
      }
    }
    error("$nodeContract class does not have OnboardingNode annotation")
  }

  private fun getOnboardingNodeMetadata(onboardingNode: OnboardingNode): OnboardingNodeMetadata {
    val nodeMetadata = onboardingNode.component.split("/")
    return when (nodeMetadata.size) {
      1 -> OnboardingNodeMetadata(nodeMetadata[0], nodeMetadata[0])
      2 -> OnboardingNodeMetadata(nodeMetadata[0], nodeMetadata[1])
      else -> error("OnboardingNode component ${onboardingNode.component} is invalid")
    }
  }

  // Creates a list of ContentValues representing the list of allowedNodes.
  private fun createContentValuesForAllowedNodes(
    allowedNodes: List<NodeData>
  ): Array<ContentValues> =
    allowedNodes
      .map { node ->
        ContentValues().apply { put(TEST_NODE_CLASS_COLUMN, node.allowedContractIdentifier) }
      }
      .toTypedArray()

  // Deletes the test configs stored all the apps listed in [appsStoringTestConfig] .
  private fun deleteAllTestConfigs(appsStoringTestConfig: Set<String>) {
    for (appPackageName in appsStoringTestConfig) {
      deleteTestConfigsOfApp(appPackageName)
    }
  }

  // Deletes the test configs stored by a given app.
  private fun deleteTestConfigsOfApp(packageName: String) {
    val uri =
      ConfigProviderUtil.getTestConfigContentUri(ConfigProviderUtil.getAuthority(packageName))
    instrumentationContext.contentResolver.delete(uri, /* where= */ null, /* selectionArgs= */ null)
  }

  /**
   * Extracts the [TestNodes] annotation applied to [description].
   *
   * There can only be a maximum of 1 such annotation, and if 0 is provided then null will be
   * returned.
   */
  private fun extractTestNodesAnnotation(description: Description) =
    description.annotations?.filterIsInstance<TestNodes>()?.firstOrNull()

  /**
   * Stores the test configuration obtained by processing [TestNodes] annotation. This data will be
   * later used to store the list of [allowedNodes] in all the apps given by [appsStoringTestConfig]
   * using their [TestContentProvider].
   */
  private data class TestNodesConfiguration(
    /**
     * Stores the package name of apps where the test configs will be stored. This includes the app
     * package name of allowed nodes and the instrumented app.
     */
    val appsStoringTestConfig: Set<String> = setOf(),

    /** Stores the list of nodes which are allowed to execute in tests. */
    val allowedNodes: List<NodeData> = listOf(),
  )
}
