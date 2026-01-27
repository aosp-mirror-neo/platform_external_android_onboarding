package com.android.onboarding.versions

import android.content.Context
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.utils.logging.OnboardingLog

/**
 * Version of [DefaultOnboardingChanges] which supports changing versioning information.
 *
 * This can only be used by components which have the capability of manipulating global settings. It
 * should generally only be used by the single component in Onboarding which controls versioning for
 * the entire flow.
 */
@InternalOnboardingApi
@RequiresApi(24)
class DefaultMutableOnboardingChanges
@JvmOverloads
constructor(
  context: Context,
  onboardingChanges: DefaultOnboardingChanges = DefaultOnboardingChanges(context),
) : BaseMutableOnboardingChanges(onboardingChanges) {
  private val applicationContext = context.applicationContext

  override fun debug(message: String) {
    log.d(message)
  }

  override fun setSettingString(key: String, value: String?) {
    Settings.Global.putString(applicationContext.contentResolver, key, value)
  }

  override fun setSettingInt(key: String, value: Int) {
    Settings.Global.putInt(applicationContext.contentResolver, key, value)
  }

  override fun enableTag(tag: String) = enableTag(applicationContext.packageName, tag)

  override fun disableTag(tag: String) = disableTag(applicationContext.packageName, tag)

  override fun enableChange(changeId: Long) = enableChange(applicationContext.packageName, changeId)

  override fun disableChange(changeId: Long) =
    disableChange(applicationContext.packageName, changeId)

  override fun setChangeEnabled(changeId: Long, enabled: Boolean) =
    setChangeEnabled(applicationContext.packageName, changeId, enabled)

  override fun resetChange(changeId: Long) = resetChange(applicationContext.packageName, changeId)

  override fun resetTag(tag: String) = resetTag(applicationContext.packageName, tag)

  private companion object {
    val log = OnboardingLog.logger("DefaultMutableOnboardingChanges")
  }
}
