package com.android.onboarding.versions

import android.content.Context
import android.provider.Settings

/** Entry point to checking if processes support particular change ids. */
interface OnboardingChanges {

  /**
   * True if the current executing process supports the given change ID.
   *
   * The changeId must reference a long constant annotated with
   * [com.android.onboarding.versions.annotations.ChangeId]
   */
  fun currentProcessSupportsChange(changeId: Long): Boolean
}

/**
 * Default implementation of [OnboardingChanges].
 *
 * For local testing, use `adb shell settings put global package.name.changeid 1` to enable and `adb
 * shell settings put global package.name.changeid 0` to disable. For example to enable change ID
 * 1234 on package `com.android.setup` we would use `adb shell settings put global
 * com.android.setup.1234 1`. You can also use `adb shell settings delete global
 * package.name.changeid` to use the default configured value.
 */
class DefaultOnboardingChanges(val context: Context) : OnboardingChanges {
  override fun currentProcessSupportsChange(changeId: Long): Boolean {
    // First we check for global setting override (for local testing)
    val changeOverride = getChangeOverride(context.packageName, changeId)
    if (changeOverride != null) {
      return changeOverride
    }

    return false
  }

  private fun getChangeOverride(packageName: String, changeId: Long): Boolean? {
    val settingKey = "$packageName.$changeId"

    return try {
      Settings.Global.getInt(context.contentResolver, settingKey) == 1
    } catch (e: Settings.SettingNotFoundException) {
      null
    }
  }
}

/** Fake implementation of [OnboardingChanges]. */
class FakeOnboardingChanges : OnboardingChanges {

  val supportedChanges = mutableSetOf<Long>()

  override fun currentProcessSupportsChange(changeId: Long): Boolean {
    return supportedChanges.contains(changeId)
  }

  fun enableChange(changeId: Long) {
    supportedChanges.add(changeId)
  }

  fun disableChange(changeId: Long) {
    supportedChanges.remove(changeId)
  }
}
