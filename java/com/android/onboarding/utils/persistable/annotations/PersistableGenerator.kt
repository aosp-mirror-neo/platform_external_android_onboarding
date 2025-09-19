package com.android.onboarding.utils.persistable.annotations

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName

/**
 * A generator for any type persistable to PersistableBundle.
 *
 * @param typeName the typesafe type name to use.
 * @param getter a PersistableBundle getter method for serialized type.
 * @param putter a PersistableBundle putter method for serialized type.
 * @param converter code converter taking [typeName] property reference and converting it to
 *   serialized type.
 * @param reverter code converter taking serialized reference and reverting it to [typeName] type.
 * @param wrapper code converter taking [typeName] reference and converting it into mirrored source
 *   type.
 * @param unwrapper a wrapper taking mirrored source type reference and converting it into
 *   [typeName] type.
 */
class PersistableGenerator(
  val typeName: TypeName,
  private val getter: String,
  private val putter: String,
  internal val converter: (CodeBlock) -> CodeBlock,
  internal val reverter: (CodeBlock) -> CodeBlock,
  private val wrapper: (CodeBlock) -> CodeBlock,
  private val unwrapper: (CodeBlock) -> CodeBlock,
) {
  /**
   * Generate a serialization [CodeBlock] which writes [property] to [bundle].
   *
   * @param bundle PersistableBundle reference.
   * @param property property reverence.
   * @param key PersistableBundle key to use.
   */
  fun serializer(bundle: CodeBlock, property: CodeBlock, key: String) =
    CodeBlock.of("%L.%L(%S, %L)", bundle, putter, key, converter(property))

  /**
   * Generate a deserialization [CodeBlock] which reads [key] from [bundle] and converts it to
   * [typeName].
   *
   * @param bundle PersistableBundle reference.
   * @param key PersistableBundle key to use.
   */
  fun deserializer(bundle: CodeBlock, key: String) =
    CodeBlock.of(
        "requireNotNull(%L.%L(%S)) { %S }",
        bundle,
        getter,
        key,
        "PersistableBundle is missing value for key: $key",
      )
      .let(reverter)

  /**
   * Wrap [source] into type specified by [typeName] assuming [typeName] has a constructor that
   * takes [source] as a single argument.
   *
   * This has no effect if [typeName] is not a [wrapper].
   *
   * This method is safe to call in any context and most of the time will just return [source].
   *
   * @param source code providing the value to wrap.
   */
  fun wrap(source: CodeBlock) = wrapper(source)

  /**
   * Unwrap [source] of [typeName] to original mirrored type.
   *
   * This method is safe to call in any context and most of the time will just return [source].
   *
   * @param source code providing the value to unwrap.
   */
  fun unwrap(source: CodeBlock) = unwrapper(source)

  @Suppress("FunctionName")
  companion object {
    fun Native(
      typeName: TypeName,
      methodName: String,
      wrapper: (CodeBlock) -> CodeBlock = { it },
      unwrapper: (CodeBlock) -> CodeBlock = { it },
    ) =
      PersistableGenerator(
        typeName = typeName,
        getter = "get$methodName",
        putter = "put$methodName",
        converter = { it },
        reverter = { it },
        wrapper = wrapper,
        unwrapper = unwrapper,
      )

    fun NativeArray(
      elementTypeName: TypeName,
      methodName: String,
      unwrapper: (CodeBlock) -> CodeBlock,
    ) =
      PersistableGenerator(
        typeName = LIST.parameterizedBy(elementTypeName),
        getter = "get$methodName",
        putter = "put$methodName",
        converter = unwrapper,
        reverter = { CodeBlock.of("%L.toList()", it) },
        wrapper = { CodeBlock.of("%L.toList()", it) },
        unwrapper = unwrapper,
      )

    fun Converted(
      typeName: TypeName,
      host: PersistableGenerator,
      converter: (CodeBlock) -> CodeBlock,
      reverter: (CodeBlock) -> CodeBlock,
      wrapper: (CodeBlock) -> CodeBlock = { it },
      unwrapper: (CodeBlock) -> CodeBlock = { it },
    ) =
      PersistableGenerator(
        typeName = typeName,
        getter = host.getter,
        putter = host.putter,
        converter = converter,
        reverter = reverter,
        wrapper = wrapper,
        unwrapper = unwrapper,
      )

    fun ConvertedArray(
      elementTypeName: TypeName,
      host: PersistableGenerator,
      converter: (CodeBlock) -> CodeBlock,
      reverter: (CodeBlock) -> CodeBlock,
      unwrapper: (CodeBlock) -> CodeBlock,
    ) =
      Converted(
        typeName = LIST.parameterizedBy(elementTypeName),
        host = host,
        converter = converter,
        reverter = reverter,
        wrapper = { CodeBlock.of("%L.toList()", it) },
        unwrapper = unwrapper,
      )

    val PString = Native(typeName = STRING, methodName = "String")
    val PInt = Native(typeName = INT, methodName = "Int")
    val PDouble = Native(typeName = DOUBLE, methodName = "Double")
    val PLong = Native(typeName = LONG, methodName = "Long")
    val PPersistableBundle =
      Native(typeName = Type.PersistableBundle, methodName = "PersistableBundle")
    val PBoolean =
      Converted(
        typeName = BOOLEAN,
        host = PString,
        converter = { CodeBlock.of("%L.toString()", it) },
        reverter = { CodeBlock.of("%L.toBoolean()", it) },
      )
    val PChar =
      Converted(
        typeName = CHAR,
        host = PString,
        converter = { CodeBlock.of("%L.toString()", it) },
        reverter = { CodeBlock.of("%L.single()", it) },
      )
    val PByte =
      Converted(
        typeName = BYTE,
        host = PInt,
        converter = { CodeBlock.of("%L.toInt()", it) },
        reverter = { CodeBlock.of("%L.toByte()", it) },
      )
    val PShort =
      Converted(
        typeName = SHORT,
        host = PInt,
        converter = { CodeBlock.of("%L.toInt()", it) },
        reverter = { CodeBlock.of("%L.toShort()", it) },
      )
    val PFloat =
      Converted(
        typeName = FLOAT,
        host = PDouble,
        converter = { CodeBlock.of("%L.toDouble()", it) },
        reverter = { CodeBlock.of("%L.toFloat()", it) },
      )

    val PStringArray =
      NativeArray(
        elementTypeName = STRING,
        methodName = "StringArray",
        unwrapper = { CodeBlock.of("%L.toTypedArray()", it) },
      )

    val PIntArray =
      NativeArray(
        elementTypeName = INT,
        methodName = "IntArray",
        unwrapper = { CodeBlock.of("%L.toIntArray()", it) },
      )

    val PDoubleArray =
      NativeArray(
        elementTypeName = DOUBLE,
        methodName = "DoubleArray",
        unwrapper = { CodeBlock.of("%L.toDoubleArray()", it) },
      )

    val PLongArray =
      NativeArray(
        elementTypeName = LONG,
        methodName = "LongArray",
        unwrapper = { CodeBlock.of("%L.toLongArray()", it) },
      )

    val PBooleanArray =
      ConvertedArray(
        elementTypeName = BOOLEAN,
        host = PStringArray,
        converter = { CodeBlock.of("%L.map(%T::toString).toTypedArray()", it, BOOLEAN) },
        reverter = { CodeBlock.of("%L.map(%T::toBoolean)", it, STRING) },
        unwrapper = { CodeBlock.of("%L.toBooleanArray()", it) },
      )
    val PCharArray =
      ConvertedArray(
        elementTypeName = CHAR,
        host = PStringArray,
        converter = { CodeBlock.of("%L.map(%T::toString).toTypedArray()", it, CHAR) },
        reverter = { CodeBlock.of("%L.map(%T::single)", it, STRING) },
        unwrapper = { CodeBlock.of("%L.toCharArray()", it) },
      )
    val PByteArray =
      ConvertedArray(
        elementTypeName = BYTE,
        host = PIntArray,
        converter = { CodeBlock.of("%L.map(%T::toInt).toIntArray()", it, BYTE) },
        reverter = { CodeBlock.of("%L.map(%T::toByte)", it, INT) },
        unwrapper = { CodeBlock.of("%L.toByteArray()", it) },
      )
    val PShortArray =
      ConvertedArray(
        elementTypeName = SHORT,
        host = PIntArray,
        converter = { CodeBlock.of("%L.map(%T::toInt).toIntArray()", it, SHORT) },
        reverter = { CodeBlock.of("%L.map(%T::toShort)", it, INT) },
        unwrapper = { CodeBlock.of("%L.toShortArray()", it) },
      )
    val PFloatArray =
      ConvertedArray(
        elementTypeName = FLOAT,
        host = PDoubleArray,
        converter = { CodeBlock.of("%L.map(%T::toDouble).toDoubleArray()", it, FLOAT) },
        reverter = { CodeBlock.of("%L.map(%T::toFloat)", it, DOUBLE) },
        unwrapper = { CodeBlock.of("%L.toFloatArray()", it) },
      )

    fun Persistable(
      type: TypeName,
      serializer: TypeName,
      deserializer: TypeName,
      wrapper: (CodeBlock) -> CodeBlock = { it },
      unwrapper: (CodeBlock) -> CodeBlock = { it },
    ) =
      Converted(
        typeName = type,
        host = PPersistableBundle,
        converter = { CodeBlock.of("%T.toPersistableBundle(%L)", serializer, it) },
        reverter = { CodeBlock.of("%T.fromPersistableBundle(%L)", deserializer, it) },
        wrapper = wrapper,
        unwrapper = unwrapper,
      )

    fun PersistableList(
      elementType: PersistableGenerator,
      unwrapper: (CodeBlock) -> CodeBlock = elementType.unwrapper,
      wrapper: (CodeBlock) -> CodeBlock = elementType.wrapper,
    ) =
      Converted(
        typeName = LIST.parameterizedBy(elementType.typeName),
        host = PPersistableBundle,
        converter = {
          CodeBlock.of(
            "%T.Strict.toPersistableBundle(%L.map { %T { %L } })",
            Type.PersistableListSerializer,
            it,
            Type.Persistable,
            elementType.converter(CodeBlock.of("it")),
          )
        },
        reverter = {
          CodeBlock.of(
            "%T({ %L }).fromPersistableBundle(%L)",
            Type.StrictPersistableListDeserializer,
            elementType.reverter(CodeBlock.of("it")),
            it,
          )
        },
        wrapper = { CodeBlock.of("%L.map { %L }", it, wrapper(CodeBlock.of("it"))) },
        unwrapper = {
          CodeBlock.of("%L.map { %L }.toTypedArray()", it, unwrapper(CodeBlock.of("it")))
        },
      )

    /**
     * Build a [PersistableGenerator] for given enum [type] which serializes [Enum].[property] as
     * [PersistableString][com.android.onboarding.utils.persistable.PersistableString] and uses
     * [kotlin.Enum.entries] to convert it back by comparing string representations of the
     * [property].
     */
    fun Enum(type: TypeName, property: String) =
      Converted(
        typeName = type,
        host = PPersistableBundle,
        converter = {
          CodeBlock.of(
            "%T.toPersistableBundle(%L.%L.toString())",
            Type.PersistableString,
            it,
            property,
          )
        },
        reverter = {
          CodeBlock.of(
            """
            |%1T.rawFromPersistableBundle(%4L).let { key ->
            |  val entries = %2T.entries
            |  val candidates = entries.filter { it.%3L.toString() == key }
            |  val entry = candidates.singleOrNull()
            |  requireNotNull(entry) {
            |    "Failed to deserialize enum %2T: expected one entry for key '${"$"}key' with property '%3L', but found ${"$"}{ candidates.size }"
            |  }
            |}"""
              .trimMargin(),
            Type.PersistableString,
            type,
            property,
            it,
          )
        },
      )
  }
}

object Type {
  val PersistableBundle = ClassName("android.os", "PersistableBundle")
  val Persistable = com.android.onboarding.utils.persistable.Persistable::class.asTypeName()
  val PersistableSerializer =
    com.android.onboarding.utils.persistable.PersistableSerializer::class.asTypeName()
  val PersistableDeserializer =
    com.android.onboarding.utils.persistable.PersistableDeserializer::class.asTypeName()
  val PersistableDelegate =
    com.android.onboarding.utils.persistable.PersistableDelegate::class.asTypeName()
  val PersistableString =
    com.android.onboarding.utils.persistable.PersistableString::class.asTypeName()
  val PersistableListSerializer =
    ClassName("com.android.onboarding.utils.persistable", "PersistableListSerializer")
  val StrictPersistableListDeserializer =
    ClassName("com.android.onboarding.utils.persistable", "StrictPersistableListDeserializer")
  val KClassMetadata =
    ClassName("com.android.onboarding.utils.persistable.annotations", "KClassMetadata")
  val IPersistableAnnotation =
    ClassName("com.android.onboarding.utils.persistable.annotations", "IPersistableAnnotation")
  val PersistableAnnotationCompanion =
    ClassName(
      "com.android.onboarding.utils.persistable.annotations",
      "PersistableAnnotationCompanion",
    )
  val PersistableAnnotationGenerated = PersistableAnnotation.Generated::class.asTypeName()
}
