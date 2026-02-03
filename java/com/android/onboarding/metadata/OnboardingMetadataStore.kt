package com.android.onboarding.metadata

import android.os.PersistableBundle
import com.android.onboarding.common.annotations.getUniqueKey
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.contentDeepEquals
import com.android.onboarding.utils.generateHash
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import java.time.Instant

/** Stores the latest versions of multiple sets of [OnboardingMetadata]. */
class OnboardingMetadataStore
private constructor(private var _lastUpdate: Instant, bundle: PersistableBundle) :
  Persistable, StableEquality {

  @PublishedApi
  internal val bundle
    get() = _bundle

  private var _bundle = deepCopy(bundle)

  constructor() : this(Instant.now(), PersistableBundle())

  /** The last update time of the store. */
  val lastUpdate
    get() = _lastUpdate

  /** True if there is no metadata in this store. */
  val isEmpty: Boolean
    get() = bundle.isEmpty

  /**
   * Gets the latest set metadata of the given type.
   *
   * Note that this will return null if the data is invalid or the given metadata has never been
   * set.
   */
  fun <T : OnboardingMetadata> get(cls: Class<T>, metadataType: PersistableDeserializer<T>): T? {
    try {
      return cls.getUniqueKey()?.let { bundle.getPersistable(it, metadataType) }
    } catch (e: Exception) {
      Log.wtf(TAG, "Error reading $cls from persistableBundle")
      return null
    }
  }

  /**
   * Gets the latest set metadata of the given type.
   *
   * Note that this will return null if the data is invalid or the given metadata has never been
   * set.
   *
   * Example usage: `metadataStore[DescriptiveTaskMetadata]`
   */
  inline operator fun <reified T : OnboardingMetadata> get(
    metadataType: PersistableDeserializer<T>
  ): T? = get(T::class.java, metadataType)

  /**
   * Replaces all stored metadata of the given type with this newer version.
   *
   * This replaces the previous value entirely, and does not merge at all.
   */
  fun <T : OnboardingMetadata> put(metadataType: Class<T>, metadata: T) {
    synchronized(bundle) {
      _lastUpdate = Instant.now()
      metadataType.getUniqueKey()?.let { bundle.putPersistable(it, metadata) }
        ?: error("Class $metadataType must be annotated with @UniqueKey")
    }
  }

  /**
   * Replaces all stored metadata of the given type with this newer version.
   *
   * This replaces the previous value entirely, and does not merge at all.
   */
  inline fun <reified T : OnboardingMetadata> put(metadata: T) {
    put(T::class.java, metadata)
  }

  /** Clears all stored metadata of the given type. */
  fun <T : OnboardingMetadata> clear(metadataType: Class<T>) {
    synchronized(bundle) {
      _lastUpdate = Instant.now()
      metadataType.getUniqueKey()?.let { bundle.remove(it) }
        ?: error("Class $metadataType must be annotated with @UniqueKey")
    }
  }

  /**
   * Replace metadata in this store with metadata from [other].
   *
   * Any metadata not present in [other] will not be modified.
   */
  fun putAll(other: OnboardingMetadataStore) {
    synchronized(bundle) {
      _lastUpdate = Instant.now()
      bundle.putAll(other.bundle)
    }
  }

  /** Clears all stored metadata of any type. */
  fun clear() {
    synchronized(bundle) {
      _lastUpdate = Instant.now()
      bundle.clear()
    }
  }

  override fun toString(): String {
    val metadataString = bundle.keySet().joinToString(", ") { key -> "$key=${bundle.get(key)}" }
    return "OnboardingMetadataStore{lastUpdate=$lastUpdate, metadata={$metadataString}}"
  }

  override fun toPersistableBundle() =
    PersistableBundle().apply {
      putLong(KEY_LAST_UPDATE, _lastUpdate.epochSecond)
      putPersistableBundle(KEY_BUNDLE, bundle)
    }

  /**
   * Resets this [OnboardingMetadataStore] from supplied state in [store].
   *
   * It does so via a shallow copy so modifying [store] later will have a side-effect of changing
   * this [OnboardingMetadataStore] state.
   */
  @InternalOnboardingApi
  fun reset(store: OnboardingMetadataStore) {
    synchronized(_bundle) {
      this._lastUpdate = store._lastUpdate
      this._bundle = store._bundle
    }
  }

  /** Make a deep copy of this object. */
  fun copy() = OnboardingMetadataStore(_lastUpdate, deepCopy(bundle))

  private fun deepCopy(bundle: PersistableBundle): PersistableBundle {
    val newBundle = PersistableBundle()
    // First we shallow copy over everything
    newBundle.putAll(bundle)

    // And then deep copy persistable bundles
    for (key in bundle.keySet()) {
      val v = bundle.get(key)
      when (v) {
        is PersistableBundle -> newBundle.putPersistableBundle(key, deepCopy(v))
      }
    }

    return newBundle
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is OnboardingMetadataStore) return false

    if (_lastUpdate.epochSecond != other._lastUpdate.epochSecond) return false
    if (!(bundle contentDeepEquals other.bundle)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = _lastUpdate.epochSecond.toInt()
    result = 31 * result + bundle.generateHash()
    return result
  }

  companion object : PersistableDeserializer<OnboardingMetadataStore> {
    const val TAG = "OnboardingMetadataStore"

    private const val KEY_LAST_UPDATE = "lastUpdate"
    private const val KEY_BUNDLE = "bundle"

    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): OnboardingMetadataStore {
      if (persistableBundle.isEmpty) {
        return OnboardingMetadataStore()
      }
      return OnboardingMetadataStore(
        Instant.ofEpochSecond(persistableBundle.getLong(KEY_LAST_UPDATE)),
        persistableBundle.getPersistableBundle(KEY_BUNDLE)
          ?: error("Missing bundle in OnboardingMetadataStore"),
      )
    }
  }
}
