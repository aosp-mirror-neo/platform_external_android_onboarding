package com.android.onboarding.utils.suwmetadata

import android.content.Context
import com.android.onboarding.utils.logging.OnboardingLog
import com.google.android.setupcompat.partnerconfig.PartnerConfigHelper

/** A default implementation of [SuwMetadataProvider]. */
class DefaultSuwMetadataProvider(private val context: Context) : SuwMetadataProvider {

  override fun getSuwSessionId(): Long {
    var sessionId = 0L
    try {
      sessionId = PartnerConfigHelper.getSuwSessionId(context)
      OnboardingLog.i(TAG, "Got SUW session ID: $sessionId")
    } catch (e: Exception) {
      OnboardingLog.w(TAG, "Failed to get SUW session id")
    }
    return sessionId
  }

  private companion object {
    const val TAG = "SuwMetadataProvider"
  }
}
