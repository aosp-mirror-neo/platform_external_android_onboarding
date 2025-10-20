package com.android.onboarding.utils.datasplitter

import android.os.Parcel
import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting
import com.android.onboarding.utils.generateHash
import com.android.onboarding.utils.persistable.sizeInBytes
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred

/** An object to hold large data that's being transferred for binder transactions. */
object BundleSplitter {
  /**
   * A map of data that's too large for binder transactions, keyed by data identifiers.
   *
   * The data is transferred in trunks alongside the main RPC calls.
   *
   * There are two use cases where this map is used:
   * 1. The server holds the large input data sent by the client.
   * 2. The client holds the large output data received from the server.
   *
   * The data should be cleared after it is consumed.
   */
  private val fullLargeDataMap = ConcurrentHashMap<Int, CompletableDeferred<PersistableBundle>>()

  /**
   * A map of data id to its incomplete data chunks being transferred.
   *
   * @see fullLargeDataMap
   */
  val chunkDataMap = ConcurrentHashMap<Int, MutableList<ByteArray>>()

  /** Returns the full large data as a [CompletableDeferred]. */
  fun getFullLargeDataDeferred(dataId: Int): CompletableDeferred<PersistableBundle> =
    fullLargeDataMap.getOrPut(dataId) { CompletableDeferred() }

  /** Removes the cached large data from [fullLargeDataMap] and [chunkDataMap]. */
  fun removeCachedData(dataId: Int) {
    val unused = fullLargeDataMap.remove(dataId)
    chunkDataMap.remove(dataId)
  }

  @VisibleForTesting
  fun reset() {
    fullLargeDataMap.clear()
    chunkDataMap.clear()
  }
}

/**
 * Joins chunks of data previously split by [splitForTransaction] into a complete
 * [PersistableBundle].
 *
 * @throws IllegalArgumentException if it fails to reassemble the full data from the chunks of data.
 */
fun persistableBundleOfChunks(chunks: List<ByteArray>): PersistableBundle {
  // List<ByteArray> -> List<Byte> -> ByteArray
  val fullDataByteArray = chunks.flatMap(ByteArray::asList).toByteArray()
  // ByteArray -> PersistableBundle
  val p = Parcel.obtain()
  val bundle: PersistableBundle?
  try {
    p.unmarshall(fullDataByteArray, 0, fullDataByteArray.size)
    p.setDataPosition(0)
    bundle = p.readPersistableBundle(PersistableBundle::class.java.classLoader)
  } catch (e: AssertionError) {
    throw IllegalArgumentException("Failed to reassemble full data from chunks of data.", e)
  } catch (e: Exception) {
    throw IllegalArgumentException("Failed to reassemble full data from chunks of data.", e)
  } finally {
    p.recycle()
  }
  requireNotNull(bundle) { "Failed to reassemble full data from chunks of data." }
  return bundle
}

/**
 * Returns whether a [PersistableBundle] is considered too large for binder transaction.
 *
 * @see android.os.TransactionTooLargeException
 * @see splitForTransaction
 * @see persistableBundleOfChunks
 */
fun PersistableBundle.isTooLargeForTransaction() =
  sizeInBytes() >= TRANSACTION_TOO_LARGE_SIZE_IN_BYTES

/**
 * Splits a [PersistableBundle] into chunks of smaller data with a maximum chunk size of
 * [chunkSize].
 *
 * These can be reassembled into a [PersistableBundle] by [persistableBundleOfChunks].
 */
fun PersistableBundle.splitForTransaction(
  chunkSize: Int = TRANSACTION_CHUNK_SIZE_IN_BYTES
): List<ByteArray> {
  val p = Parcel.obtain()
  try {
    p.writePersistableBundle(this)
    val fullDataByteArray = p.marshall()
    return fullDataByteArray.asList().chunked(chunkSize).map(List<Byte>::toByteArray)
  } finally {
    p.recycle()
  }
}

/**
 * Creates a pending bundle using the content as the identifier.
 *
 * When a [PersistableBundle] is too large for binder transaction, it can be sent by splitting into
 * chunks with [splitForTransaction], and reassembled in the receiver side with
 * [persistableBundleOfChunks].
 *
 * When this happens, this pending bundle can be sent as a placeholder to the receiver to signal it
 * to fetch the full data with the [getPendingDataIdentifier].
 */
fun PersistableBundle.toPendingBundle(): PersistableBundle =
  PersistableBundle().apply {
    putInt(KEY_PENDING_LARGE_DATA_IDENTIFIER, this@toPendingBundle.generateHash())
    putInt(KEY_PENDING_LARGE_DATA_SIZE_IN_BYTES, this@toPendingBundle.sizeInBytes())
    putInt(KEY_PENDING_LARGE_DATA_TOTAL_CHUNKS, this@toPendingBundle.numOfChunks())
  }

/**
 * Removes the pending bundle extras added by [toPendingBundle].
 *
 * As a result, this [PersistableBundle] will no longer be a pending bundle.
 */
@CanIgnoreReturnValue
fun PersistableBundle.removePendingBundleExtras(): PersistableBundle {
  remove(KEY_PENDING_LARGE_DATA_IDENTIFIER)
  remove(KEY_PENDING_LARGE_DATA_SIZE_IN_BYTES)
  remove(KEY_PENDING_LARGE_DATA_TOTAL_CHUNKS)
  return this
}

/**
 * Returns if the bundle is a pending bundle.
 *
 * @see toPendingBundle
 */
fun PersistableBundle.isPendingBundle() = this.containsKey(KEY_PENDING_LARGE_DATA_IDENTIFIER)

/**
 * Returns the identifier of the pending bundle.
 *
 * This is unique with respect to the full data content this pending bundle represents.
 *
 * @throws [IllegalArgumentException] if the caller is not a pending bundle.
 * @see toPendingBundle
 */
fun PersistableBundle.getPendingDataIdentifier(): Int {
  require(isPendingBundle()) { "This bundle $this is not a pending bundle." }
  return this.getInt(KEY_PENDING_LARGE_DATA_IDENTIFIER)
}

/**
 * Returns the total data chunks of the full data content for the pending bundle.
 *
 * This is unique with respect to the full data content this pending bundle represents.
 *
 * @throws [IllegalArgumentException] if the caller is not a pending bundle.
 * @see toPendingBundle
 */
fun PersistableBundle.getPendingDataTotalChunks(): Int {
  require(isPendingBundle()) { "This bundle $this is not a pending bundle." }
  return this.getInt(KEY_PENDING_LARGE_DATA_TOTAL_CHUNKS)
}

/**
 * Returns the data size in bytes of the full data content for the pending bundle.
 *
 * This is unique with respect to the full data content this pending bundle represents.
 *
 * @throws [IllegalArgumentException] if the caller is not a pending bundle.
 * @see toPendingBundle
 */
fun PersistableBundle.getPendingDataSizeInBytes(): Int {
  require(isPendingBundle()) { "This bundle $this is not a pending bundle." }
  return this.getInt(KEY_PENDING_LARGE_DATA_SIZE_IN_BYTES)
}

/**
 * Returns the total data chunks of the full data content when it's split for binder transaction.
 *
 * @see splitForTransaction
 */
@VisibleForTesting fun PersistableBundle.numOfChunks(): Int = splitForTransaction().size

private const val TRANSACTION_CHUNK_SIZE_IN_BYTES: Int = 50000 // 50Kb
@VisibleForTesting const val TRANSACTION_TOO_LARGE_SIZE_IN_BYTES: Int = 500_000 // 500Kb
@VisibleForTesting
const val KEY_PENDING_LARGE_DATA_IDENTIFIER: String = "PendingLargeDataIdentifier"
@VisibleForTesting
const val KEY_PENDING_LARGE_DATA_TOTAL_CHUNKS: String = "PendingLargeDataTotalChunks"
@VisibleForTesting
const val KEY_PENDING_LARGE_DATA_SIZE_IN_BYTES: String = "PendingLargeDataSizeInBytes"
