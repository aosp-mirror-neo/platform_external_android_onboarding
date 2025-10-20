package com.android.onboarding.utils.persistable

import android.os.PersistableBundle

/** [Persistable] wrapper around [Pair]. */
@JvmInline
value class PersistablePair<A : Persistable, B : Persistable>(val pair: Pair<A, B>) : Persistable {

  constructor(first: A, second: B) : this(Pair(first, second))

  override fun toPersistableBundle() = toPersistableBundle(pair)

  private class Deserializer<A : Persistable, B : Persistable>(
    val firstDeserializer: PersistableDeserializer<A>,
    val secondDeserializer: PersistableDeserializer<B>,
  ) :
    PersistableDeserializer<PersistablePair<A, B>>,
    PersistableSerializer<Pair<A, B>> by PersistablePair {
    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      PersistablePair(
        Pair(
          persistableBundle.getPersistable(KEY_FIRST, firstDeserializer)
            ?: error("Missing first from PersistablePair"),
          persistableBundle.getPersistable(KEY_SECOND, secondDeserializer)
            ?: error("Missing second from PersistablePair"),
        )
      )
  }

  companion object : PersistableSerializer<Pair<Persistable, Persistable>> {
    private const val KEY_FIRST = "first"
    private const val KEY_SECOND = "second"

    fun <A : Persistable, B : Persistable> deserializer(
      firstDeserializer: PersistableDeserializer<A>,
      secondDeserializer: PersistableDeserializer<B>,
    ): PersistableDeserializer<PersistablePair<A, B>> =
      Deserializer(firstDeserializer, secondDeserializer)

    override fun toPersistableBundle(value: Pair<Persistable, Persistable>): PersistableBundle =
      PersistableBundle {
        putPersistable(KEY_FIRST, value.first)
        putPersistable(KEY_SECOND, value.second)
      }
  }
}
