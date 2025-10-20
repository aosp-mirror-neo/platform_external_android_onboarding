package com.android.onboarding.versions.change_collector

import com.android.onboarding.versions.annotations.BreakingReason
import com.android.onboarding.versions.annotations.ChangeId
import com.android.onboarding.versions.annotations.ChangeId.Companion.generateChangeIdCode
import com.android.onboarding.versions.annotations.ChangeRadius
import com.android.onboarding.versions.annotations.ExpeditedReason
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import java.io.IOException
import java.io.PrintWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.StandardLocation

/**
 * Annotation processor which collects all @ChangeId annotations and indexes them in a fixed map.
 *
 * The map is called [com.android.onboarding.versions.changes.ALL_CHANGE_IDS], as has a (Long) key
 * of the bugId, with the value being the [ChangeId] itself.
 */
@SupportedAnnotationTypes("com.android.onboarding.versions.annotations.ChangeId")
@AutoService(javax.annotation.processing.Processor::class)
class ChangeCollector : AbstractProcessor() {
  override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

  private val changeIds = mutableListOf<ChangeIdInfo>()

  data class ChangeIdInfo(val name: String, val changeId: ChangeId)

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    val annotatedIds = roundEnv.getElementsAnnotatedWith(ChangeId::class.java)
    for (annotatedId in annotatedIds) {
      changeIds.add(
        ChangeIdInfo(
          annotatedId.getSimpleName().toString(),
          annotatedId.getAnnotation(ChangeId::class.java),
        )
      )
    }

    if (roundEnv.processingOver()) {
      val changesClass = ClassName("com.android.onboarding.versions.changes", "AllChanges")

      val fileBuilder =
        FileSpec.scriptBuilder(
          "com.android.onboarding.versions.changes.AllChanges",
          "com.android.onboarding.versions.changes",
        )

      fileBuilder.addImport(
        ChangeRadius::class.qualifiedName!!,
        ChangeRadius.entries.map { it.name }.toList(),
      )
      fileBuilder.addImport(
        ExpeditedReason::class.qualifiedName!!,
        ExpeditedReason.entries.map { it.name }.toList(),
      )
      fileBuilder.addImport(
        BreakingReason::class.qualifiedName!!,
        BreakingReason.entries.map { it.name }.toList(),
      )
      fileBuilder.addImport(ChangeId::class.java.packageName, ChangeId::class.java.simpleName)

      val file =
        fileBuilder
          .addCode(
            CodeBlock.builder()
              .addStatement(
                "val ALL_CHANGE_IDS = mapOf(" +
                  changeIds
                    .map { "${it.changeId.bugId}L to ${generateChangeIdCode(it.changeId)}" }
                    .joinToString(",\n") +
                  ")"
              )
              .addStatement(
                "val CHANGE_NAME = mapOf(" +
                  changeIds.map { "${it.changeId.bugId}L to \"${it.name}\"" }.joinToString(",\n") +
                  ")"
              )
              .build()
          )
          .build()

      try {
        val builderFile =
          processingEnv.filer.createResource(
            StandardLocation.SOURCE_OUTPUT,
            changesClass.packageName,
            "${changesClass.simpleName}.kt",
          )
        PrintWriter(builderFile.openWriter()).use { out -> file.writeTo(out) }
      } catch (e: IOException) {
        throw java.lang.IllegalStateException("Error writing change map to file", e)
      }
    }
    return false
  }
}
