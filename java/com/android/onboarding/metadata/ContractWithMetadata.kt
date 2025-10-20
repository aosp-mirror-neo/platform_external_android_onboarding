package com.android.onboarding.metadata

/** Interface for Onboarding contracts that support Metadata. */
interface ContractWithMetadata<M> {

  /**
   * Extracts Metadata from [M] into a [List] of [EncodedMetadata] allowing metadata to be logged.
   *
   * @param metadata the metadata associated with this task contract which needs to be encoded
   * @return [List] of [EncodedMetadata] from provided [metadata]
   */
  fun encodeMetadata(metadata: M): List<EncodedMetadata>
}
