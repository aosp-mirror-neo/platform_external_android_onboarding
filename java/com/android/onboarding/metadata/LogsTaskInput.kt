package com.android.onboarding.metadata

/**
 * Provides capability to log an onboarding task's input.
 *
 * @param I the type of the task input.
 */
interface LogsTaskInput<in I> {
  /**
   * Encodes task arguments into a privacy non-sensitive metadata list of [EncodedMetadata]. The
   * default implementation delegates to [taskArgs] if it implements [MetadataWriter], otherwise it
   * returns empty metadata.
   *
   * @param taskArgs The task arguments to encode.
   * @return The encoded list of metadata.
   */
  fun encodeArgsIntoMetadata(taskArgs: I): List<EncodedMetadata> =
    if (taskArgs is MetadataWriter) taskArgs.toNonSensitiveMetadata() else listOf()
}
