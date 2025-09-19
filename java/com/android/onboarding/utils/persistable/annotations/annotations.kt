package com.android.onboarding.utils.persistable.annotations

import kotlin.reflect.KClass

/**
 * Marks target annotation as [com.android.onboarding.utils.persistable.Persistable] which will
 * trigger a companion type to be generated which has all the same properties as marked annotation
 * and can be persisted. Generated companion type is named as "[prefix]<ANNOTATION_NAME>[suffix]".
 *
 * @property prefix Specifies a prefix to prepend to the generated type name. Defaults to `"P"`.
 * @property suffix Specifies a suffix to append to the generated type name. Defaults to none.
 * @property lossy Is any data in the annotation non-reversible. `false` by default. If `false` it
 *   instructs the generator to implement [AnnotationProvider] and be stricter during property
 *   validation. Marked annotations are usually [lossy] if they contain [kotlin.reflect.KClass] or
 *   [Annotation] parameters. All other cases usually could be marked as non-[lossy].
 * @property id an optional annotation ID tu use when querying [KClassMetadata]. If left empty,
 *   [KClass.qualifiedName] of the marked annotation will be used. This is mainly useful for keeping
 *   backwards compatibility when renaming or moving the annotation.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class PersistableAnnotation(
  val prefix: String = "P",
  val suffix: String = "",
  val lossy: Boolean = false,
  val id: String = "",
) {
  /**
   * Custom [PersistableBundle][android.os.PersistableBundle] key for annotated property.
   *
   * If not specified, property name will be used by default.
   *
   * ```kotlin
   * @PersistableAnnotation
   * annotation class ExampleAnnotation(
   *   val value1: Int // will use "value1" PersistableBundle key.
   *   @Key("Noice!") val value2: Int // will use "Noice!" PersistableBundle key.
   * )
   * ```
   */
  @Target(AnnotationTarget.PROPERTY_GETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Key(val key: String)

  /**
   * Specifies which property should be used to serialize the annotated [Enum] parameter.
   *
   * Annotation only has effect when used on parameters of [Enum] type.
   *
   * All [Enum] parameters must either be annotated with this annotation or implement
   * [Persistable][com.android.onboarding.utils.persistable.Persistable] or
   * [PersistableEnum][com.android.onboarding.utils.persistable.PersistableEnum] type.
   *
   * ```kotlin
   * enum class ExampleEnum(val id: Int) { A(10), B(20) }
   *
   * @PersistableAnnotation
   * annotation class ExampleAnnotation(
   *   @get:EnumKey("id")
   *   val enumValue: ExampleEnum
   *   @get:EnumKey("id")
   *   val enumArray: Array<ExampleEnum>
   * )
   * ```
   *
   * @property property A member property of the annotated type which to use for [Any.toString]
   *   serialization to key.
   */
  @Target(AnnotationTarget.PROPERTY_GETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class EnumKey(val property: String)

  /**
   * Specifies which annotations should be extracted from the annotated [KClass] parameter.
   *
   * Annotation only has effect when used on properties of [KClass] or Array<[KClass] type.
   *
   * Since direct [KClass] serialization is not supported, all [KClass] parameters must be annotated
   * with this annotation specifying which other annotations should be extracted. Annotated types
   * will be converted to [KClassMetadata] for serialization.
   *
   * Use of this annotation implies that parent annotation is [PersistableAnnotation.lossy].
   *
   * ```kotlin
   * @PersistableAnnotation
   * enum class AnotherExampleAnnotation(val name: String)
   *
   * abstract class MyContractBase
   *
   * @AnotherExampleAnnotation("contract")
   * class MyContract: MyContractBase()
   *
   * @PersistableAnnotation
   * annotation class ExampleAnnotation(
   *   @get:ExtractMetadata(AnotherExampleAnnotation::class)
   *   val kClassValue: KClass<out MyContractBase>
   *   @get:ExtractMetadata(AnotherExampleAnnotation::class)
   *   val kClassArray: Array<KClass<out MyContractBase>>
   * )
   * ```
   *
   * @property annotations An array of annotations to extract from marked [KClass]. All specified
   *   annotations must themselves be marked with @[PersistableAnnotation].
   */
  @Target(AnnotationTarget.PROPERTY_GETTER)
  @Retention(AnnotationRetention.SOURCE)
  annotation class ExtractMetadata(vararg val annotations: KClass<out Annotation>)

  /**
   * Internal annotation for the generator to store some runtime metadata.
   *
   * WARNING: DO NOT USE
   */
  @Target(AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class Generated(val sourceId: String)
}

/**
 * An interface implemented by generated [PersistableAnnotation] implementations to allow reverting
 * back to the real annotation where possible.
 *
 * @see PersistableAnnotation.lossy
 */
fun interface AnnotationProvider<A : Annotation> {
  /** Reconstruct original annotation [A]. */
  fun toAnnotation(): A
}
