package com.android.onboarding.versions

import android.content.Context
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.ComponentRef
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
  /**
   * @param component the [ComponentRef] of the "current" component owning this instance for
   *   implicit change checks.
   */
  constructor(
    context: Context,
    component: ComponentRef,
  ) : this(context, DefaultOnboardingChanges(context, component))

  private val applicationContext = context.applicationContext

  /** @see DefaultOnboardingChanges.owner */
  val owner: ComponentRef = onboardingChanges.owner

  override fun debug(message: String) {
    log.d(message)
  }

  override fun setSettingString(key: String, value: String?) {
    Settings.Global.putString(applicationContext.contentResolver, key, value)
  }

  override fun setSettingInt(key: String, value: Int) {
    Settings.Global.putInt(applicationContext.contentResolver, key, value)
  }

  override fun enableTag(tag: String) = enableTag(owner, tag)

  override fun disableTag(tag: String) = disableTag(owner, tag)

  override fun enableChange(changeId: Long) = enableChange(owner, changeId)

  override fun disableChange(changeId: Long) = disableChange(owner, changeId)

  override fun setChangeEnabled(changeId: Long, enabled: Boolean) =
    setChangeEnabled(owner, changeId, enabled)

  override fun resetChange(changeId: Long) = resetChange(owner, changeId)

  override fun resetTag(tag: String) = resetTag(owner, tag)

  private companion object {
    val log = OnboardingLog.logger("DefaultMutableOnboardingChanges")
  }
}
