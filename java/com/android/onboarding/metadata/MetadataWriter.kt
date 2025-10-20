package com.android.onboarding.metadata

/**
 * Extracts and sanitizes the contents of the implementing object into non-sensitive metadata that
 * can be logged safely.
 */
interface MetadataWriter {
  /**
   * Extracts and sanitizes the contents of this object into a non-sensitive list of
   * [EncodedMetadata].
   */
  fun toNonSensitiveMetadata(): List<EncodedMetadata>
}
