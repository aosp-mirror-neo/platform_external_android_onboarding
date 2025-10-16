package com.android.onboarding.utils.suwmetadata

/** A class to get SUW metadata. */
interface SuwMetadataProvider {

  /**
   * Gets the SUW sessions ID.
   *
   * Returns 0 if it failed to get the SUW session ID.
   */
  fun getSuwSessionId(): Long
}
