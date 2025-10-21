package com.android.onboarding.tasks.service

import android.os.PersistableBundle
import com.android.onboarding.utils.datasplitter.persistableBundleOfChunks
import com.android.onboarding.utils.datasplitter.splitForTransaction
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import kotlinx.coroutines.yield

/**
 * A stub implementation of [IPersistableBundleStream] with predefined data.
 *
 * Note that this stream is "read-once".
 *
 * @property chunks All data chunks this stream should send.
 * @see IPersistableBundleStream
 */
class PersistableBundleStream(private val chunks: List<ByteArray>) :
  IPersistableBundleStream.Stub() {
  constructor(persistableBundle: PersistableBundle) : this(persistableBundle.splitForTransaction())

  constructor(persistable: Persistable) : this(persistable.toPersistableBundle())

  override fun version() = 1

  private var nextChunk = 0

  /** @see IPersistableBundleStream.readNextChunk */
  override fun readNextChunk(): ByteArray? = chunks.getOrNull(nextChunk++)
}

/**
 * Await for all data in the [IPersistableBundleStream] and return it as deserialized
 * [PersistableBundle].
 *
 * This operation consumes the stream and further [await] calls with the same
 * [IPersistableBundleStream] will return empty [PersistableBundle].
 */
@JvmSynthetic
suspend fun IPersistableBundleStream.await(): PersistableBundle {
  val chunks = mutableListOf<ByteArray>()
  var chunk: ByteArray?
  do {
    chunk = readNextChunk()
    if (chunk != null) chunks += chunk
    yield()
  } while (chunk != null)
  return persistableBundleOfChunks(chunks)
}

/** Reads this [IPersistableBundleStream] as [T] via [deserializer]. */
@JvmSynthetic
suspend fun <T> IPersistableBundleStream.asPersistable(
  deserializer: PersistableDeserializer<T>
): T = deserializer.fromPersistableBundle(await())
