package com.android.onboarding.utils.persistable

import android.os.Bundle
import android.os.PersistableBundle
import com.android.onboarding.encoding.Encoder
import unsupported


/** Interface used to indicate an object can be represented as a [PersistableBundle]. */
fun interface Persistable {
  /** Returns a [PersistableBundle] representing this object. */
  fun toPersistableBundle(): PersistableBundle

  companion object : PersistableSerializer<Persistable>, PersistableDeserializer<Persistable> {
    override fun toPersistableBundle(value: Persistable): PersistableBundle =
      value.toPersistableBundle()

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): Persistable =
      Persistable {
        persistableBundle
      }
  }
}

/** Returns a [Bundle] representing this object. */
@JvmName("toBundleOrNull") fun Persistable?.toBundle(): Bundle? = unsupported

/** Returns a [Bundle] representing this object. */
fun Persistable.toBundle(): Bundle = unsupported

/**
 * Interface used for companion objects to [Persistable] types to convert from a
 * [PersistableBundle].
 *
 * Expected usage:
 * ```kotlin
 * class MyClass(val a1: String, val a2:String) : Persistable {
 *   override fun toPersistableBundle(): PersistableBundle =
 *     PersistableBundle().also { putString("a1", a1) putString("a2", a2) }
 *
 *   companion object : PersistableDeserializer<MyClass> {
 *       override fun fromPersistableBundle(persistableBundle: PersistableBundle): MyClass =
 *           MyClass(a1 = persistableBundle.getString("a1"), a2 = persistableBundle.getString("a2"))
 *   }
 * }
 * ```
 */
fun interface PersistableDeserializer<out Type> : Encoder<PersistableBundle, Type> {
  /** Returns an instance of [Type] as represented by the [PersistableBundle]. */
  fun fromPersistableBundle(persistableBundle: PersistableBundle): Type

  override fun encode(input: PersistableBundle): Type = fromPersistableBundle(input)
}

/**
 * A [Persistable.toPersistableBundle] conversion shorthand via [PersistableDeserializer].
 *
 * @see Persistable.toPersistableBundle
 */
@Suppress("UnusedReceiverParameter", "NOTHING_TO_INLINE")
inline fun <P : Persistable> PersistableDeserializer<P>.toPersistableBundle(
  value: P
): PersistableBundle = value.toPersistableBundle()

/**
 * Interface used for defining serializers for types that cannot themselves be [Persistable] types
 * to convert them to a [PersistableBundle].
 */
fun interface NullablePersistableSerializer<in T> {
  /** Returns an instance of [PersistableBundle] representing the [value]. */
  fun toPersistableBundle(value: T): PersistableBundle?

  /** Converts this serializer to also accept nullable [T] values. */
  fun withNullableValues(): NullablePersistableSerializer<T?> = NullablePersistableSerializer {
    it?.let(this::toPersistableBundle)
  }
}

/**
 * Interface used for defining serializers for types that cannot themselves be [Persistable] types
 * to convert them to a [PersistableBundle].
 */
fun interface PersistableSerializer<in Type> : NullablePersistableSerializer<Type> {
  override fun toPersistableBundle(value: Type): PersistableBundle
}

/**
 * A delegate implementing [Persistable] serialization logic for types that cannot be [Persistable]
 * directly.
 */
interface PersistableDelegate<Type> : PersistableSerializer<Type>, PersistableDeserializer<Type>

/** Writes a [Persistable] to the [Bundle]. */
fun Bundle.putPersistable(key: String, data: Persistable?) = putBundle(key, data?.toBundle())

/** Writes a [Persistable] to the [PersistableBundle]. */
fun PersistableBundle.putPersistable(key: String, data: Persistable?) =
  putPersistableBundle(key, data?.toPersistableBundle())

