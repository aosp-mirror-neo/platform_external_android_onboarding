package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import kotlin.enums.EnumEntries

/**
 * Persistable implementation for [Enum] types using [id] property for better backwards
 * compatibility.
 *
 * Sample implementation:
 * ```kotlin
 * enum class MyEnum: PersistableEnum {
 *   ONE,TWO, UNKNOWN;
 *
 *   override val id = name
 *
 *   companion object: PersistableDeserializer<MyEnum> by PersistableEnum.deserializer(entries, UNKNOWN)
 * }
 * ```
 */
interface PersistableEnum : Persistable {
  /**
   * Persistence identifier for this enum value.
   *
   * It should never be changes once set to maintain backwards compatibility. Most of the time
   * initial implementations can use either [Enum.ordinal] or [Enum.name] and switch it up to an
   * actual property when backwards compatibility becomes important.
   */
  val id: String

  override fun toPersistableBundle() = PersistableString.toPersistableBundle(id)

  companion object : PersistableSerializer<PersistableEnum> {

    override fun toPersistableBundle(value: PersistableEnum) = value.toPersistableBundle()

    /**
     * Builds a deserializer for [T].
     *
     * Most implementations should use delegation:
     * ```kotlin
     * enum class MyEnum: PersistableEnum {
     *   ONE,TWO, UNKNOWN;
     *
     *   override val id = name
     *
     *   companion object: PersistableDeserializer<MyEnum> by PersistableEnum.deserializer(entries, UNKNOWN)
     * }
     * ```
     *
     * @param entries will almost always be a call to `T.entries` property of the enum type.
     * @param default the default value to use when none of the [entries] [PersistableEnum.id] match
     *   the persisted value. Usually only relevant when a newer version of the enum serialized the
     *   value and an older version tried to read it.
     */
    fun <T> deserializer(entries: EnumEntries<T>, default: T): PersistableDeserializer<T>
      where T : Enum<T>, T : PersistableEnum =
      object : PersistableDeserializer<T> {
        override fun fromPersistableBundle(persistableBundle: PersistableBundle): T {
          val id = PersistableString.rawFromPersistableBundle(persistableBundle)
          return entries.firstOrNull { it.id == id } ?: default
        }
      }

    /**
     * Builds a deserializer for [T] that throws an IllegalStateException if a bundle cannot be
     * deserialized as [T].
     *
     * @param entries will almost always be a call to `T.entries` property of the enum type.
     */
    fun <T> deserializer(entries: EnumEntries<T>): PersistableDeserializer<T>
      where T : Enum<T>, T : PersistableEnum =
      object : PersistableDeserializer<T> {
        override fun fromPersistableBundle(persistableBundle: PersistableBundle): T {
          val id = PersistableString.rawFromPersistableBundle(persistableBundle)
          return requireNotNull(entries.firstOrNull { it.id == id }) {
            "Failed to deserialize enum: could not find id $id"
          }
        }
      }
  }
}
