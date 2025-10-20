package com.android.onboarding.testing

import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.UniqueNodeRef
import com.android.onboarding.nodes.refersTo
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth

class ComponentRefSubject
private constructor(metadata: FailureMetadata, private val actual: ComponentRef?) :
  Subject(metadata, actual) {

  private fun refersTo(actual: ComponentRef?, expected: ComponentRef): Boolean {
    if (actual == null) return false
    return when {
      expected is UniqueNodeRef && actual is UniqueNodeRef -> expected.refersTo(actual)
      expected is NodeRef && actual is NodeRef -> expected.refersTo(actual)
      else -> expected.refersTo(actual)
    }
  }

  /** Check that actual refers to [expected]. */
  fun refersTo(expected: ComponentRef) {
    if (!refersTo(actual, expected)) {
      failWithoutActual(
        fact("expected", actual?.identity()),
        fact("to refer to", expected.identity()),
        simpleFact("but it did not"),
      )
    }
  }

  /** Check that actual does not refer to [expected]. */
  fun doesNotReferTo(expected: ComponentRef) {
    if (refersTo(actual, expected)) {
      failWithoutActual(
        fact("expected", actual?.identity()),
        fact("to not refer to", expected.identity()),
        simpleFact("but it did"),
      )
    }
  }

  companion object : Factory<ComponentRefSubject, ComponentRef> {
    override fun createSubject(
      metadata: FailureMetadata,
      actual: ComponentRef?,
    ): ComponentRefSubject = ComponentRefSubject(metadata, actual)

    @JvmStatic
    fun assertThat(actual: ComponentRef): ComponentRefSubject =
      Truth.assertAbout(ComponentRefSubject).that(actual)
  }
}
