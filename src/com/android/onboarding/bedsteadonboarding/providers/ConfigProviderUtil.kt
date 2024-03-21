package com.android.onboarding.bedsteadonboarding.providers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri

/**
 * Utility class for [TestContentProvider]. This contains constants representing content provider
 * paths and also functions on creating uri using those.
 */
object ConfigProviderUtil {
  /**
   * The relative path for content provider representing all test configurations. This should be
   * used to delete all the configs at once.
   */
  internal const val TEST_CONFIG_PATH = "test_config"

  /** The base path for content provider for querying fake activity node configs. */
  private const val FAKE_ACTIVITY_NODE_CONFIG_QUERY_BASE_PATH = "fake_activity_node_config"

  /** The relative path of the content provider for querying fake activity node configs. */
  internal const val FAKE_ACTIVITY_NODE_CONFIG_QUERY_PATH =
    "$FAKE_ACTIVITY_NODE_CONFIG_QUERY_BASE_PATH/*"

  /** The relative path of the content provider for inserting fake activity node configs. */
  internal const val FAKE_ACTIVITY_NODE_CONFIG_INSERT_PATH = "fake_activity_node_config"

  /** The projection/ column name representing one single allowed node in a test. */
  const val TEST_NODE_CLASS_COLUMN = "allowed_node"

  /**
   * Returns the base content provider [Uri]. Eg: content://authority
   *
   * @param authority the content provider authority to use for constructing Uri
   * @return the base content provider path.
   */
  fun getBaseContentUri(authority: String): Uri =
    Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).build()

  /**
   * Returns the uri for all the test configuration Eg: content://authority/test_config
   *
   * @param authority the content provider authority to use for constructing Uri
   */
  fun getTestConfigUri(authority: String): Uri =
    getBaseContentUri(authority).buildUpon().path(TEST_CONFIG_PATH).build()

  /**
   * Returns the uri for all the test configuration Eg: content://authority/test_config
   *
   * @param context context of the application being executed
   */
  fun getTestConfigUri(context: Context): Uri = getTestConfigUri(getAuthority(context.packageName))

  /**
   * Returns the uri for fetching the fake activity node configs for node identified by
   * [contractIdentifier].
   */
  fun getFakeActivityNodeConfigQueryUri(context: Context, contractIdentifier: String): Uri =
    getBaseContentUri(getAuthority(context.packageName))
      .buildUpon()
      .path(FAKE_ACTIVITY_NODE_CONFIG_QUERY_BASE_PATH)
      .appendPath(contractIdentifier)
      .build()

  /**
   * Returns the uri for inserting the fake activity node config. This will be stored using the
   * content provider of the app with given [packageName].
   */
  fun getFakeActivityNodeConfigInsertionUri(packageName: String): Uri =
    getBaseContentUri(getAuthority(packageName))
      .buildUpon()
      .path(FAKE_ACTIVITY_NODE_CONFIG_INSERT_PATH)
      .build()

  // LINT.IfChange(authority)
  /**
   * Given app package name it returns content provider authority for a given package
   *
   * @param packageName the app package name
   * @return the content provider authority for a given package
   */
  fun getAuthority(packageName: String): String = "$packageName.testprovider"
  // LINT.ThenChange(src/com/android/onboarding/bedsteadonboarding/AndroidManifest.xml:authority)
}
