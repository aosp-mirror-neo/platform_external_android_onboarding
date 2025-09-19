package com.android.onboarding.utils.persistable.annotations

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

@SupportedAnnotationTypes(
  "com.android.onboarding.utils.persistable.annotations.PersistableAnnotation"
)
@AutoService(javax.annotation.processing.Processor::class)
class PersistableAnnotationProcessor : AbstractProcessor() {
  override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

  private val annotatedElements = mutableSetOf<TypeElement>()

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val specs =
      roundEnv
        .getElementsAnnotatedWith(PersistableAnnotation::class.java)
        .filter { it.kind == ElementKind.ANNOTATION_TYPE }
        .filterIsInstance<TypeElement>()
        .mapNotNull(::generateFile)

    for (spec in specs) {
      try {
        spec.writeTo(processingEnv.filer)
      } catch (e: Exception) {
        processingEnv.messager.printError("Failed to write file: $e")
      }
    }
    return false
  }

  private fun generateFile(declaration: TypeElement): FileSpec? {
    val meta = declaration.getAnnotation(PersistableAnnotation::class.java) ?: return null
    val generator =
      PersistableAnnotationGenerator(
        processingEnv = processingEnv,
        meta = meta,
        sourceDeclaration = declaration,
      )
    val type = generator.generate() ?: return null
    return FileSpec.builder(generator.genType).addType(type).build()
  }
}
