package com.android.onboarding.tasks.service;

/**
 * A stream of data representing a PersistableBundle.
 *
 * This allows working around the android IPC data size limitations by chunking
 * the total data and sending it over in smaller pieces.
 *
 * NOTE: This stream is "read-once" and each chunk is consumed during reads.
 */
interface IPersistableBundleStream {
  /**
   * Version of this implementation to know which methods are available.
   *
   * @since 1
   */
  int version() = 0;

  /**
   * Read and consume next chunk in this stream.
   *
   * @return The next chunk, or null if there are no more chunks.
   * @since 1
   */
  @nullable byte[] readNextChunk() = 1;
}
