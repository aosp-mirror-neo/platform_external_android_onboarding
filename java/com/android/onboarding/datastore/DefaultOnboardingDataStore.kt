package com.android.onboarding.datastore

import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.preferencesDataStore
import com.android.onboarding.utils.datasplitter.persistableBundleOfChunks
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableSerializer
import com.android.onboarding.utils.persistable.toByteArray
import com.android.onboarding.utils.suwmetadata.DefaultSuwMetadataProvider
import com.android.onboarding.utils.suwmetadata.SuwMetadataProvider
import com.android.onboarding.versions.DefaultOnboardingChanges
import com.android.onboarding.versions.OnboardingChanges
import com.android.onboarding.versions.changes.SETUP_SESSION_BASED_STORAGE_CHANGE_ID
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.withContext


/**
 * A default implementation of [OnboardingDataStore] that stores data in a [DataStore] for the
 * current SUW session.
 *
 * The data for a previous session is cleared as soon as the DataStore is accessed in a new session.
 * Sessions outside SUW, where a SUW session id is not available, is considered as a special single
 * session. That means the DataStore is cleared when it's accessed between SUW and non-SUW sessions.
 * And that the DataStore won't be cleared as long as it's accessed outside SUW.
 *
 * Clients must not create multiple DataStores with the same file name. If there are multiple
 * DataStores active for a given file in the same process, DataStore will throw
 * [IllegalStateException] when reading or updating data.
 *
 * Complex clients, e.g. those with submodules, are encouraged to provide unique [keyPrefix] for
 * each different instances of [DefaultOnboardingDataStore] to minimize the risk of key name
 * collision. Alternatively, you can provide different custom [dataStore], which allows customizing
 * file corruption handling on top of namespace collision.
 *
 * @param coroutineScope Coroutine scope to be used for launching async tasks and creating
 *   [ListenableFuture].
 * @param dataStore An optional singleton [DataStore] to store data on. Otherwise a default
 *   datastore of file name "OnboardingDataStore" will be used. Your datastore must not have the
 *   file name "OnboardingDataStore" or "OnboardingTaskDataStore", which are reserved for onboarding
 *   infrastructure uses.
 * @param keyPrefix A key prefix to reduce the chance of key name collision, especially when the
 *   default datastore is being used.
 */

class DefaultOnboardingDataStore(
  context: Context,
  private val suwMetadataProvider: SuwMetadataProvider = DefaultSuwMetadataProvider(context),
  private val onboardingChanges: OnboardingChanges = DefaultOnboardingChanges(context),
  private val coroutineScope: CoroutineScope,
  private val dataStore: DataStore<Preferences> = context.defaultDataStore,
  private val keyPrefix: String,
) : OnboardingDataStore {
  private val context =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      Log.e("tmmmm", "Creating device protected storage context")
      context.applicationContext.createDeviceProtectedStorageContext()
    } else {
      Log.e("tmmmm", "Creating normal storage context")
      context.applicationContext
    }
  private var cachedLastAccessedSuwSessionId: Long = 0L

  override suspend fun <T> write(key: String, value: T, serializer: PersistableSerializer<T>) {
    if (!onboardingChanges.currentProcessSupportsChange(SETUP_SESSION_BASED_STORAGE_CHANGE_ID)) {
      return
    }
    withContext(coroutineScope.coroutineContext) {
      writePersistableBundle(key, serializer.toPersistableBundle(value))
    }
  }

  override suspend fun <T : Persistable> write(key: String, value: T) =
    write(key, value, Persistable::toPersistableBundle)

  override fun <T> writeAsync(
    key: String,
    value: T,
    serializer: PersistableSerializer<T>,
  ): ListenableFuture<Unit> = coroutineScope.future { write(key, value, serializer) }

  override fun <T : Persistable> writeAsync(key: String, value: T): ListenableFuture<Unit> =
    writeAsync(key, value, Persistable::toPersistableBundle)

  override suspend fun <T> read(key: String, deserializer: PersistableDeserializer<T>): T? {
    if (!onboardingChanges.currentProcessSupportsChange(SETUP_SESSION_BASED_STORAGE_CHANGE_ID)) {
      return null
    }
    return withContext(coroutineScope.coroutineContext) {
      readPersistableBundle(key)?.let { deserializer.fromPersistableBundle(it) }
    }
  }

  override fun <T> readAsync(
    key: String,
    deserializer: PersistableDeserializer<T>,
  ): ListenableFuture<T?> = coroutineScope.future { read(key, deserializer) }

  private suspend fun writePersistableBundle(key: String, value: PersistableBundle) {
    val keyWithPrefix = appendPrefixIfNecessary(key)
    try {
      getDataStore().edit { it[byteArrayPreferencesKey(keyWithPrefix)] = value.toByteArray() }
    } catch (e: Exception) {
      Log.e(TAG, "Ignoring error", e)
    }
  }

  private suspend fun readPersistableBundle(key: String): PersistableBundle? {
    val keyWithPrefix = appendPrefixIfNecessary(key)
    var bundle: PersistableBundle? = null
    try {
      bundle =
        getDataStore().data.firstOrNull()?.get(byteArrayPreferencesKey(keyWithPrefix))?.let {
          persistableBundleOfChunks(listOf(it))
        }
    } catch (e: Exception) {
      Log.e(TAG, "Ignoring error", e)
    }
    return bundle
  }

  private fun appendPrefixIfNecessary(key: String): String =
    if (key.startsWith(keyPrefix)) {
      key
    } else {
      keyPrefix + key
    }

  /** Gets the [DataStore] for the current SUW session. */
  private suspend fun getDataStore(): DataStore<Preferences> {
    val suwSessionId = suwMetadataProvider.getSuwSessionId()
    if (suwSessionId == 0L) {
      Log.w(TAG, "Failed to get SUW session id. Returning DataStore as is.")
      return dataStore
    }

    // Accessing a new SUW session means the previous one is concluded. Clear it.
    if (suwSessionId != getLastAccessedSuwSessionId()) {
      try {
        Log.d(TAG, "Preparing dataStore")
        context.defaultDataStore.edit { it.clear() }
        dataStore.edit { it.clear() }
        persistSuwSessionId(suwSessionId)
        Log.d(TAG, "Cleared DataStore for SUW session $suwSessionId")
      } catch (e: Exception) {
        Log.e(TAG, "Ignoring error", e)
      }
    }

    return dataStore
  }

  /** Persists the last accessed SUW session ID to both memory and disk. */
  private suspend fun persistSuwSessionId(suwSessionId: Long) {
    cachedLastAccessedSuwSessionId = suwSessionId
    context.defaultDataStore.edit {
      it[longPreferencesKey(KEY_LAST_ACCESSED_SUW_SESSION_ID)] = suwSessionId
    }
  }

  /** Gets the last accessed SUW session ID from either memory or disk. */
  private suspend fun getLastAccessedSuwSessionId(): Long {
    if (cachedLastAccessedSuwSessionId != 0L) {
      return cachedLastAccessedSuwSessionId
    }

    context.defaultDataStore.data
      .first()[longPreferencesKey(KEY_LAST_ACCESSED_SUW_SESSION_ID)]
      ?.let { cachedLastAccessedSuwSessionId = it }
    return cachedLastAccessedSuwSessionId
  }

  private companion object {
    const val TAG = "OnboardingDataStore"

    const val DATA_STORE_NAME = "OnboardingDataStore"

    const val KEY_LAST_ACCESSED_SUW_SESSION_ID = "last_accessed_suw_session_id"

    /**
     * A singleton default [DataStore] that stores the SUW session ID. This will also be used to
     * store app data if the optional [dataStore] is not provided. This must be a singleton for a
     * file name and process.
     */
    val Context.defaultDataStore: DataStore<Preferences> by
      preferencesDataStore(
        name = DATA_STORE_NAME,
        corruptionHandler = ReplaceFileCorruptionHandler { mutablePreferencesOf() },
      )
  }
}
