package com.android.onboarding.utils.persistable

import android.os.PersistableBundle

internal const val KEY_SIZE = "size"
internal const val KEY_ITEM_PREFIX = "item_"
internal const val KEY_KEY_PREFIX = "key_"

/** A [PersistableSerializer] for [List]. */
open class PersistableListSerializer<T>(
  private val elementSerializer: NullablePersistableSerializer<T>
) : PersistableSerializer<List<T>> {
  final override fun toPersistableBundle(value: List<T>) = PersistableBundle {
    putInt(KEY_SIZE, value.size)
    value.forEachIndexed { i, persistable ->
      putPersistable("$KEY_ITEM_PREFIX$i", persistable, elementSerializer)
    }
  }

  companion object : PersistableListSerializer<Persistable?>({ it?.toPersistableBundle() }) {
    /** Strict [PersistableListSerializer] not allowing null list elements. */
    val Strict = PersistableListSerializer(Persistable)
  }
}

/** A [PersistableDeserializer] for [List]. */
open class StrictPersistableListDeserializer<T>(
  private val elementDeserializer: PersistableDeserializer<T>,
  private val onMissing: (index: Int) -> T = {
    error("Missing persistable list value at index $it")
  },
) : PersistableDeserializer<List<T>> {
  final override fun fromPersistableBundle(persistableBundle: PersistableBundle) = buildList {
    val size = persistableBundle.getInt(KEY_SIZE)
    for (i in 0..<size) {
      add(
        persistableBundle.getPersistable("$KEY_ITEM_PREFIX$i", elementDeserializer) ?: onMissing(i)
      )
    }
  }
}

/** A [PersistableDeserializer] for [List]. */
open class PersistableListDeserializer<T>(elementDeserializer: PersistableDeserializer<T>) :
  StrictPersistableListDeserializer<T?>(elementDeserializer, { null })

/** A [PersistableDelegate] for [List]. */
open class PersistableListDelegate<T>(
  elementSerializer: NullablePersistableSerializer<T>,
  elementDeserializer: PersistableDeserializer<T>,
) : PersistableDelegate<List<T?>> {
  constructor(elementDelegate: PersistableDelegate<T>) : this(elementDelegate, elementDelegate)

  private val listSerializer =
    PersistableListSerializer<T?> { it?.let(elementSerializer::toPersistableBundle) }
  private val listDeserializer = PersistableListDeserializer(elementDeserializer)

  final override fun toPersistableBundle(value: List<T?>) =
    listSerializer.toPersistableBundle(value)

  final override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
    listDeserializer.fromPersistableBundle(persistableBundle)
}

/** A [PersistableSerializer] for [Map]. */
open class PersistableMapSerializer<K, V>(
  private val keySerializer: NullablePersistableSerializer<K>,
  private val valueSerializer: NullablePersistableSerializer<V>,
) : PersistableSerializer<Map<out K, V>> {
  final override fun toPersistableBundle(value: Map<out K, V>) = PersistableBundle {
    putInt(KEY_SIZE, value.size)
    for ((i, entry) in value.entries.withIndex()) {
      putPersistable("$KEY_KEY_PREFIX$i", entry.key, keySerializer)
      putPersistable("$KEY_ITEM_PREFIX$i", entry.value, valueSerializer)
    }
  }

  companion object : PersistableMapSerializer<Persistable, Persistable>(Persistable, Persistable)
}

/** A [PersistableDeserializer] for [Map]. */
open class PersistableMapDeserializer<K, V>(
  private val keyDeserializer: PersistableDeserializer<K>,
  private val valueDeserializer: PersistableDeserializer<V>,
) : PersistableDeserializer<Map<out K, V>> {
  final override fun fromPersistableBundle(persistableBundle: PersistableBundle) = buildMap {
    val size = persistableBundle.getInt(KEY_SIZE)
    for (i in 0..<size) {
      val key =
        persistableBundle.getPersistable("$KEY_KEY_PREFIX$i", keyDeserializer)
          ?: error("Missing key in persistable map $i")
      val value =
        persistableBundle.getPersistable("$KEY_ITEM_PREFIX$i", valueDeserializer)
          ?: error("Missing value in persistable map $i")
      put(key, value)
    }
  }
}

/** A [PersistableDelegate] for [Map]. */
open class PersistableMapDelegate<K, V>(
  keySerializer: NullablePersistableSerializer<K>,
  keyDeserializer: PersistableDeserializer<K>,
  valueSerializer: NullablePersistableSerializer<V>,
  valueDeserializer: PersistableDeserializer<V>,
) : PersistableDelegate<Map<out K, V>> {
  constructor(
    keyDelegate: PersistableDelegate<K>,
    elementDelegate: PersistableDelegate<V>,
  ) : this(keyDelegate, keyDelegate, elementDelegate, elementDelegate)

  private val mapSerializer = PersistableMapSerializer(keySerializer, valueSerializer)
  private val mapDeserializer = PersistableMapDeserializer(keyDeserializer, valueDeserializer)

  final override fun toPersistableBundle(value: Map<out K, V>) =
    mapSerializer.toPersistableBundle(value)

  final override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
    mapDeserializer.fromPersistableBundle(persistableBundle)
}
