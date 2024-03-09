package com.android.onboarding.testing

import com.google.common.truth.Fact.fact
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue

/** A [Truth] [Subject] to assert that no [Throwable]s were thrown failing lazily otherwise. */
class SuccessSubject
private constructor(metadata: FailureMetadata, private val actual: (() -> Any?)?) :
  Subject(metadata, actual) {

  @CanIgnoreReturnValue
  fun doesNotFail(): Subject {
    if (actual == null) {
      failWithoutActual(
        simpleFact("expected to not fail"),
        simpleFact("but no action was provided"),
      )
      return assertThat(null as Any?)
    }
    val result =
      runCatching(actual).onFailure {
        failWithoutActual(
          simpleFact("expected to not fail"),
          fact("but failed with", it::class.qualifiedName),
        )
      }
    return assertThat(result.getOrNull())
  }

  companion object : Factory<SuccessSubject, () -> Any?> {
    override fun createSubject(metadata: FailureMetadata, actual: (() -> Any?)?): SuccessSubject =
      SuccessSubject(metadata, actual)
  }
}

/** @see assertDoesNotFail */
fun StandardSubjectBuilder.doesNotFail(action: () -> Any?) {
  about(SuccessSubject).that(action).doesNotFail()
}

/**
 * Asserts that a given [action] does not throw an exception. If the [action] does throw an
 * exception, it is registered lazily to the [StandardSubjectBuilder] allowing the rest of the test
 * assertions to continue.
 *
 * Unfortunately @[CanIgnoreReturnValue] is not supported on inline library functions and as such
 * this utility function is not returning [ThrowableSubject] for further assertions. If you need
 * further assertions consider using one of the more verbose forms:
 * ```
 * @get:Rule val expect: Expect = Expect.create()
 * expect.about(SuccessSubject).that { functionUnderTest() }.doesNotFail().isNotNull()
 *
 * assertAbout(SuccessSubject).that { functionUnderTest() }.doesNotFail().isNotNull()
 * ```
 */
fun assertDoesNotFail(action: () -> Any?) {
  assertAbout(SuccessSubject).that(action).doesNotFail()
}
