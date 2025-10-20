package com.android.onboarding.metadata

/**
 * Provides capability to log an onboarding task's result.
 *
 * @param R the type of the result.
 */
interface LogsTaskResult<in R> {
  /**
   * Encodes task result into a privacy non-sensitive metadata list of [EncodedMetadata]. The
   * default implementation delegates to [result] if it implements [MetadataWriter], otherwise it
   * returns empty metadata.
   *
   * @param result the task result to encode.
   * @return The encoded list of metadata.
   */
  fun encodeResultIntoMetadata(result: R): List<EncodedMetadata> =
    if (result is MetadataWriter) result.toNonSensitiveMetadata() else listOf()
}
