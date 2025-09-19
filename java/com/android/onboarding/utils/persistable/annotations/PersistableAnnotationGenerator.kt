package com.android.onboarding.utils.persistable.annotations

import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableEnum
import com.android.onboarding.utils.persistable.PersistableSerializer
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KProperty

@OptIn(DelicateKotlinPoetApi::class)
class PersistableAnnotationGenerator(
  private val processingEnv: ProcessingEnvironment,
  private val meta: PersistableAnnotation,
  private val sourceDeclaration: TypeElement,
) {
  private val types = processingEnv.typeUtils
  private val elements = processingEnv.elementUtils
  @OptIn(DelicateKotlinPoetApi::class)
  private val sourceType: ClassName = sourceDeclaration.asClassName()
  val genType = serializerName(sourceDeclaration, meta)

  fun generate(): TypeSpec? =
    runCatching(::generateType)
      .onFailure {
        processingEnv.messager.printError(
          "Generation failed.\n${it.stackTraceToString()}",
          sourceDeclaration,
        )
      }
      .getOrNull()

  private fun generateType(): TypeSpec {
    assume("Is annotation declaration") { sourceDeclaration.kind == ElementKind.ANNOTATION_TYPE }
    val enclosedElements = sourceDeclaration.enclosedElements
    val propertyDeclarations =
      enclosedElements
        .filter { it.kind == ElementKind.METHOD }
        .filterIsInstance<ExecutableElement>()
        .associateBy { it.simpleName }

    val properties =
      propertyDeclarations.values.map { method ->
        val generator = withDeclaration(method) { selectTypeGenerator(method, method.returnType) }
        AnnotationProperty(
          processingEnv = processingEnv,
          parent = sourceType,
          propertyDeclaration = method,
          generator = generator,
        )
      }
    val builder =
      TypeSpec.classBuilder(genType)
        .addSuperinterface(Type.Persistable)
        .addModifiers(KModifier.DATA)

    val sourceId = generateSourceId(sourceDeclaration, meta)
    AnnotationSpec.builder(Type.PersistableAnnotationGenerated)
      .addMember("%L = %S", PersistableAnnotation.Generated::sourceId.name, sourceId)
      .build()
      .also(builder::addAnnotation)

    implementPrimaryConstructor(builder, properties)
    implementSourceConstructor(builder, properties)
    implementPersistable(builder, properties)
    implementCompanionObject(builder, properties)
    implementAnnotationProvider(builder, properties)

    builder.addOriginatingElement(sourceDeclaration)
    processingEnv.elementUtils
      .getDocComment(sourceDeclaration)
      ?.trim()
      ?.takeIf(String::isNotBlank)
      ?.plus("\n---\n")
      ?.also(builder::addKdoc)
    builder.addKdoc(
      """
      |Generated from [%1T]
      |
      |@see %1T"""
        .trimMargin(),
      sourceType,
    )
    for (property in properties) {
      if (property.kDocShort != null) {
        builder.addKdoc("\n@property %L %L", property.name, property.kDocShort)
      }
    }
    return builder.build()
  }

  private fun implementPrimaryConstructor(
    builder: TypeSpec.Builder,
    properties: List<AnnotationProperty>,
  ) {
    val constructorBuilder = FunSpec.constructorBuilder()
    for (property in properties) {
      constructorBuilder.addParameter(property.name, property.type)
      PropertySpec.builder(property.name, property.type)
        .addKdoc(property.kDocFull)
        .initializer(property.name)
        .build()
        .also(builder::addProperty)
    }
    builder.primaryConstructor(constructorBuilder.build())
  }

  private fun implementSourceConstructor(
    builder: TypeSpec.Builder,
    properties: List<AnnotationProperty>,
  ) {
    val sourceName = "source"
    val thisArgs =
      properties.map {
        CodeBlock.of(
          "%L = %L",
          it.name,
          it.generator.wrap(CodeBlock.of("%L.%L", sourceName, it.name)),
        )
      }
    FunSpec.constructorBuilder()
      .addParameter(sourceName, sourceType)
      .callThisConstructor(thisArgs)
      .addKdoc(
        """
        |Construct [%2T] from [%1L] values.
        |
        |@param %1L actual annotation to use for value extraction."""
          .trimMargin(),
        sourceName,
        genType,
      )
      .build()
      .also(builder::addFunction)
  }

  private fun implementAnnotationProvider(
    builder: TypeSpec.Builder,
    properties: List<AnnotationProperty>,
  ) {
    if (meta.lossy) return
    builder.addSuperinterface(AnnotationProvider::class.asTypeName().parameterizedBy(sourceType))
    val parameters =
      properties.map {
        CodeBlock.of("%L = %L", it.name, it.generator.unwrap(CodeBlock.of("%L", it.name)))
      }
    val code = CodeBlock.builder().add("return %T(", sourceType)
    for (parameter in parameters) code.add("\n\t%L,", parameter)
    code.add("\n)")
    FunSpec.builder(AnnotationProvider<*>::toAnnotation.name)
      .returns(sourceType)
      .addCode(code.build())
      .addModifiers(KModifier.OVERRIDE)
      .build()
      .also(builder::addFunction)
  }

  private fun implementPersistable(
    builder: TypeSpec.Builder,
    properties: List<AnnotationProperty>,
  ) {
    builder.addSuperinterface(Type.IPersistableAnnotation)
    val bundleName = CodeBlock.of("bundle")
    val code = CodeBlock.builder().addStatement("val %L = %T()", bundleName, Type.PersistableBundle)
    for (property in properties) {
      code.addStatement(
        "%L",
        property.generator.serializer(
          bundleName,
          CodeBlock.of(property.name),
          property.persistableKey,
        ),
      )
    }
    code.addStatement("return bundle")

    FunSpec.builder(Persistable::toPersistableBundle.name)
      .returns(Type.PersistableBundle)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(code.build())
      .build()
      .also(builder::addFunction)
  }

  private fun implementCompanionObject(
    builder: TypeSpec.Builder,
    properties: List<AnnotationProperty>,
  ) {
    val companionBuilder = TypeSpec.companionObjectBuilder()
    implementPersistableSerializer(companionBuilder)
    implementPersistableDeserializer(companionBuilder, properties)
    companionBuilder.addSuperinterface(
      Type.PersistableAnnotationCompanion.parameterizedBy(sourceType, genType)
    )
    implementRawPersistableDelegate(companionBuilder)
    builder.addType(companionBuilder.build())
  }

  private fun implementRawPersistableDelegate(builder: TypeSpec.Builder) {
    if (meta.lossy) return
    val property =
      PropertySpec.builder("Delegate", Type.PersistableDelegate.parameterizedBy(sourceType))
        .addKdoc(
          """
          |A [%T] for direct [%T] serialization and deserialization."""
            .trimMargin(),
          Type.PersistableDelegate,
          sourceType,
        )
        .addAnnotation(JvmField::class)
        .initializer(
          """
          |object: %2T<%1T>, %3T<%1T> by %4T {
          |  override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
          |    %4T.fromPersistableBundle(persistableBundle).toAnnotation()
          |}"""
            .trimMargin(),
          sourceType,
          Type.PersistableDelegate,
          Type.PersistableSerializer,
          genType,
        )
    builder.addProperty(property.build())
  }

  private fun implementPersistableSerializer(builder: TypeSpec.Builder) {
    builder.addSuperinterface(Type.PersistableSerializer.parameterizedBy(sourceType))
    val sourceName = "value"
    FunSpec.builder(PersistableSerializer<*>::toPersistableBundle.name)
      .addParameter(sourceName, sourceType)
      .returns(Type.PersistableBundle)
      .addModifiers(KModifier.OVERRIDE)
      .addCode("return %T(%L).toPersistableBundle()", genType, sourceName)
      .build()
      .also(builder::addFunction)
  }

  private fun implementPersistableDeserializer(
    builder: TypeSpec.Builder,
    properties: List<AnnotationProperty>,
  ) {
    builder.addSuperinterface(Type.PersistableDeserializer.parameterizedBy(genType))
    val bundleName = CodeBlock.of("persistableBundle")
    val code = CodeBlock.builder().add("return %T(", genType)
    for (property in properties) {
      code.add(
        "\n\t%L = %L,",
        property.name,
        property.generator.deserializer(bundleName, property.persistableKey),
      )
    }
    code.add("\n)")
    FunSpec.builder(PersistableDeserializer<*>::fromPersistableBundle.name)
      .addParameter(bundleName.toString(), Type.PersistableBundle)
      .returns(genType)
      .addModifiers(KModifier.OVERRIDE)
      .addCode(code.build())
      .build()
      .also(builder::addFunction)
  }

  private fun selectTypeGenerator(
    property: ExecutableElement,
    typeMirror: TypeMirror,
  ): PersistableGenerator {
    if (typeMirror.kind == TypeKind.ARRAY) {
      return selectArrayGenerator(property, typeMirror)
    }
    val typeElement = types.asElement(typeMirror) as? TypeElement
    if (typeElement != null) {
      if (typeElement.kind == ElementKind.ENUM) return selectEnumGenerator(property, typeMirror)
      if (typeElement.kind == ElementKind.ANNOTATION_TYPE)
        return selectAnnotationGenerator(typeMirror)
      if (
        (typeMirror as? DeclaredType)?.asElement() == elements.getTypeElement("java.lang.Class")
      ) {
        return selectKClassGenerator(property, typeMirror)
      }
    }

    return when (typeMirror.kind) {
      TypeKind.BOOLEAN -> PersistableGenerator.PBoolean
      TypeKind.BYTE -> PersistableGenerator.PByte
      TypeKind.SHORT -> PersistableGenerator.PShort
      TypeKind.INT -> PersistableGenerator.PInt
      TypeKind.LONG -> PersistableGenerator.PLong
      TypeKind.CHAR -> PersistableGenerator.PChar
      TypeKind.FLOAT -> PersistableGenerator.PFloat
      TypeKind.DOUBLE -> PersistableGenerator.PDouble
      TypeKind.ARRAY -> selectArrayGenerator(property, typeMirror)
      TypeKind.DECLARED -> {
        val typeElement = (typeMirror as DeclaredType).asElement()
        if (typeElement.kind == ElementKind.ENUM) {
          selectEnumGenerator(property, typeMirror)
        } else if (typeElement.kind == ElementKind.ANNOTATION_TYPE) {
          selectAnnotationGenerator(typeMirror)
        } else if (typeElement == elements.getTypeElement("java.lang.String")) {
          PersistableGenerator.PString
        } else if (typeElement == elements.getTypeElement("java.lang.Class")) {
          selectKClassGenerator(property, typeMirror)
        } else {
          fail("Declared Type(${typeMirror}) not supported")
        }
      }
      else -> {
        fail("Type(${typeMirror}) not supported")
      }
    }
  }

  private fun selectArrayGenerator(
    property: ExecutableElement,
    typeMirror: TypeMirror,
  ): PersistableGenerator {
    assume("Is array type") { typeMirror.kind == TypeKind.ARRAY }
    val arrayType = typeMirror as ArrayType
    val elementType = arrayType.componentType

    return when (elementType.kind) {
      TypeKind.BOOLEAN -> PersistableGenerator.PBooleanArray
      TypeKind.BYTE -> PersistableGenerator.PByteArray
      TypeKind.SHORT -> PersistableGenerator.PShortArray
      TypeKind.INT -> PersistableGenerator.PIntArray
      TypeKind.LONG -> PersistableGenerator.PLongArray
      TypeKind.CHAR -> PersistableGenerator.PCharArray
      TypeKind.FLOAT -> PersistableGenerator.PFloatArray
      TypeKind.DOUBLE -> PersistableGenerator.PDoubleArray
      TypeKind.DECLARED -> {
        val typeElement = (elementType as DeclaredType).asElement()
        if (typeElement.kind == ElementKind.ENUM) {
          PersistableGenerator.PersistableList(
            elementType = selectEnumGenerator(property, elementType)
          )
        } else if (typeElement.kind == ElementKind.ANNOTATION_TYPE) {
          PersistableGenerator.PersistableList(elementType = selectAnnotationGenerator(elementType))
        } else if (typeElement == elements.getTypeElement("java.lang.String")) {
          PersistableGenerator.PStringArray
        } else if (typeElement == elements.getTypeElement("java.lang.Class")) {
          PersistableGenerator.PersistableList(
            elementType = selectKClassGenerator(property, elementType)
          )
        } else {
          fail("Declared ArrayType(${elementType}) not supported")
        }
      }
      else -> {
        fail("ArrayType(${elementType}) not supported")
      }
    }
  }

  private inline fun <reified T> Element.isChildOf(): Boolean = asType().isChildOf<T>()

  private inline fun <reified T> TypeMirror.isChildOf(): Boolean =
    types.isAssignable(this, elements.getTypeElement(T::class.java.name).asType())

  private fun selectEnumGenerator(
    property: ExecutableElement,
    typeMirror: TypeMirror,
  ): PersistableGenerator {
    val typeElement = types.asElement(typeMirror) as TypeElement
    val enumKey = property.getAnnotation(PersistableAnnotation.EnumKey::class.java)?.property
    val typeName = typeMirror.asTypeName()
    return if (enumKey != null) {
      PersistableGenerator.Enum(typeName, enumKey)
    } else if (typeElement.isChildOf<PersistableEnum>() || typeElement.isChildOf<Persistable>()) {
      PersistableGenerator.Persistable(
        type = typeName,
        serializer = Type.Persistable,
        deserializer = typeName,
      )
    } else {
      fail(
        "Enum parameter $property is not annotated with" +
          " ${PersistableAnnotation.EnumKey::class.simpleName} and does not" +
          " implement ${Persistable::class.simpleName} nor ${PersistableEnum::class.simpleName}."
      )
    }
  }

  private fun selectAnnotationGenerator(typeMirror: TypeMirror): PersistableGenerator {
    val typeDeclaration = (types.asElement(typeMirror) as? TypeElement).assumeNotNull()
    assume("Is annotation") { typeDeclaration.kind == ElementKind.ANNOTATION_TYPE }
    val typeMeta =
      typeDeclaration
        .getAnnotation(PersistableAnnotation::class.java)
        .assumeNotNull(
          "Annotation parameter is annotated with ${PersistableAnnotation::class.simpleName}"
        )
    assume("Annotation type is not more lossy than host") { meta.lossy || !typeMeta.lossy }
    val actualType = serializerName(typeDeclaration, typeMeta)
    return PersistableGenerator.Persistable(
      type = actualType,
      serializer = Type.Persistable,
      deserializer = actualType,
      wrapper = { CodeBlock.of("%T(%L)", actualType, it) },
      unwrapper = { CodeBlock.of("%L.%L()", it, AnnotationProvider<*>::toAnnotation.name) },
    )
  }

  private fun selectKClassGenerator(
    property: ExecutableElement,
    typeMirror: TypeMirror,
  ): PersistableGenerator {
    assume("Types with KClass references are marked as lossy") { meta.lossy }
    val extractAnnotationMirror =
      property
        .getAnnotationMirror<PersistableAnnotation.ExtractMetadata>()
        .assumeNotNull(
          "Is annotated with ${PersistableAnnotation.ExtractMetadata::class.simpleName}"
        )
    val targetAnnotationTypes =
      extractAnnotationMirror
        .getAnnotationValue<List<AnnotationValue>>(
          PersistableAnnotation.ExtractMetadata::annotations
        )
        .assumeNotNull()
    val targetData: List<Triple<ClassName, ClassName, String>> =
      targetAnnotationTypes.map { type ->
        val declaration =
          ((type.value as? DeclaredType)?.asElement() as? TypeElement).assumeNotNull()
        val meta =
          declaration
            .getAnnotation(PersistableAnnotation::class.java)
            .assumeNotNull(
              "Extraction target ${declaration.qualifiedName} is marked as ${PersistableAnnotation::class.simpleName}"
            )
        val key = generateSourceId(declaration, meta)
        Triple(declaration.asClassName(), serializerName(declaration, meta), key)
      }

    val targets = CodeBlock.builder().add("arrayOf(")
    val converters = CodeBlock.builder().add("mapOf(")
    val keys = CodeBlock.builder().add("mapOf(")
    for ((type, serializerName, key) in targetData) {
      targets.add("%T::class,", type)
      converters.add("%1T::class to { %2T(it as %1T) },", type, serializerName)
      keys.add("%T::class to %S,", type, key)
    }
    targets.add(")")
    converters.add(")")
    keys.add(")")

    val actualType = Type.KClassMetadata
    return PersistableGenerator.Persistable(
      type = actualType,
      serializer = Type.Persistable,
      deserializer = Type.KClassMetadata,
      wrapper = {
        CodeBlock.of(
          "%T.extract(source = %L, targets = %L, converters = %L, keys = %L)",
          actualType,
          it,
          targets.build(),
          converters.build(),
          keys.build(),
        )
      },
    )
  }

  private fun serializerName(annotation: TypeElement, meta: PersistableAnnotation) =
    ClassName(
      elements.getPackageOf(annotation).qualifiedName.toString(),
      "${meta.prefix}${annotation.simpleName}${meta.suffix}",
    )

  private fun generateSourceId(declaration: TypeElement, meta: PersistableAnnotation): String {
    return meta.id.ifEmpty { declaration.qualifiedName.toString() }
  }

  private var currentDeclaration: Element = sourceDeclaration

  @CanIgnoreReturnValue
  private fun <R> withDeclaration(declaration: Element, action: () -> R): R {
    val prev = currentDeclaration
    currentDeclaration = declaration
    return try {
      action()
    } finally {
      currentDeclaration = prev
    }
  }

  private fun fail(message: String, declaration: Element = currentDeclaration): Nothing {
    val msg = "[${declaration}] Generator failed: $message"
    processingEnv.messager.printError(msg, declaration)
    error(msg)
  }

  @CanIgnoreReturnValue
  private inline fun <T> T.assume(
    assumption: String,
    declaration: Element? = null,
    condition: T.() -> Boolean,
  ): T {
    return if (!condition()) {
      val el = declaration ?: currentDeclaration
      val msg = "[${el}] Assumption failed: $assumption"
      processingEnv.messager.printError(msg, el)
      error(msg)
    } else {
      this
    }
  }

  @CanIgnoreReturnValue
  private fun <T> T?.assumeNotNull(
    message: String = "Required element is not null.",
    declaration: Element? = null,
  ): T & Any = checkNotNull(assume(message, declaration) { this != null })

  private inline fun <reified A : Annotation> AnnotatedConstruct.getAnnotationMirror():
    AnnotationMirror? = getAnnotationMirror(A::class.java.simpleName)

  private fun AnnotatedConstruct.getAnnotationMirror(simpleName: String): AnnotationMirror? {
    return annotationMirrors.singleOrNull {
      it.annotationType.asElement().simpleName.contentEquals(simpleName)
    }
  }

  private inline fun <reified V> AnnotationMirror.getAnnotationValue(property: KProperty<*>): V? =
    getAnnotationValue(property.name)?.value?.let { it as V }

  private fun AnnotationMirror.getAnnotationValue(valueName: String?): AnnotationValue? {
    return elementValues.entries
      .singleOrNull { (key, _) -> key.simpleName.contentEquals(valueName) }
      ?.value
  }

  private class AnnotationProperty(
    processingEnv: ProcessingEnvironment,
    parent: TypeName,
    propertyDeclaration: ExecutableElement,
    val generator: PersistableGenerator,
  ) {
    val name: String = propertyDeclaration.simpleName.toString()
    val type: TypeName = generator.typeName
    val kDocShort: CodeBlock? =
      processingEnv.elementUtils
        .getDocComment(propertyDeclaration)
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.let(CodeBlock::of)
    val kDocFull: CodeBlock =
      CodeBlock.builder()
        .apply {
          if (kDocShort != null) {
            add(kDocShort)
            add("\n---\n")
          }
          addStatement("@see %T.%L", parent, name)
        }
        .build()
    val persistableKey: String =
      propertyDeclaration.getAnnotation(PersistableAnnotation.Key::class.java)?.key ?: name
  }
}
