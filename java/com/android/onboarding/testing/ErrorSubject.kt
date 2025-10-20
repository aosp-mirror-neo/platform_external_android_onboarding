package com.android.onboarding.testing

import com.google.common.truth.FailureMetadata
import com.google.common.truth.SimpleSubjectBuilder
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler

/** A [Truth] [Subject] to assert expected [Throwable]s being thrown. */
class ErrorSubject
private constructor(metadata: FailureMetadata, private val actual: (() -> Any?)?) :
  Subject(metadata, actual) {

  @CanIgnoreReturnValue
  inline fun <reified T : Throwable> failsWith(): ThrowableSubject = failsWith(T::class)

  @CanIgnoreReturnValue
  fun <T : Throwable> failsWith(expected: KClass<T>): ThrowableSubject {
    if (actual == null) {
      failWithoutActual(
        fact("expected to fail with", expected.qualifiedName),
        simpleFact("but no action was provided"),
      )
      return assertThat(null as Throwable?)
    }
    val error = runCatching(actual).exceptionOrNull()
    if (error == null) {
      failWithoutActual(
        fact("expected to fail with", expected.qualifiedName),
        simpleFact("but did not fail"),
      )
    } else if (!expected.java.isInstance(error)) {
      failWithoutActual(
        fact("expected to fail with", expected.qualifiedName),
        fact("but failed with", error::class.qualifiedName),
      )
    }
    return assertThat(error)
  }

  companion object : Factory<ErrorSubject, () -> Any?> {
    override fun createSubject(metadata: FailureMetadata, actual: (() -> Any?)?): ErrorSubject =
      ErrorSubject(metadata, actual)
  }
}

/** @see assertFailsWith */
inline fun <reified T : Throwable> StandardSubjectBuilder.failsWith(noinline action: () -> Any?) {
  about(ErrorSubject).that(action).failsWith<T>()
}

/**
 * Asserts that the given [action] fails with the expected [Throwable] [T]
 *
 * Alternative to other forms of [ErrorSubject] usage.
 *
 * ```
 * @get:Rule val expect: Expect = Expect.create()
 * expect.about(ErrorSubject).that { functionUnderTest() }.failsWith<Throwable>().isNotNull()
 *
 * assertAbout(ErrorSubject).that { functionUnderTest() }.failsWith<Throwable>().isNotNull()
 * ```
 */
@CanIgnoreReturnValue
inline fun <reified T : Throwable> assertFailsWith(noinline action: () -> Any?): ThrowableSubject =
  assertAbout(ErrorSubject).that(action).failsWith<T>()

/**
 * Suspend version of [assertFailsWith] which uses [runBlocking] with current given
 * [coroutineContext].
 *
 * @see assertFailsWith
 * @see thatAsync
 */
@CanIgnoreReturnValue
inline fun <reified T : Throwable> CoroutineScope.assertFailsWith(
  noinline action: suspend CoroutineScope.() -> Any?
): ThrowableSubject {
  return assertAbout(ErrorSubject).thatAsync(coroutineContext, action).failsWith<T>()
}

/**
 * Suspend version of [SimpleSubjectBuilder.that] which uses [runBlocking] with current given
 * [coroutineContext].
 */
suspend fun SimpleSubjectBuilder<ErrorSubject, () -> Any?>.thatAsync(
  action: suspend CoroutineScope.() -> Any?
): ErrorSubject = thatAsync(coroutineContext + SupervisorJob(), action)

/**
 * Suspend version of [SimpleSubjectBuilder.that] which uses [runBlocking] with current given
 * [coroutineContext].
 */
fun SimpleSubjectBuilder<ErrorSubject, () -> Any?>.thatAsync(
  coroutineContext: CoroutineContext,
  action: suspend CoroutineScope.() -> Any?,
): ErrorSubject {
  return that {
    runBlocking {
      val errorAsync =
        async(context = coroutineContext, start = CoroutineStart.UNDISPATCHED) {
          try {
            action()
            null
          } catch (e: Throwable) {
            if (e is CancellationException) throw e
            e
          }
        }
      coroutineContext[TestCoroutineScheduler]?.advanceUntilIdle()
      val error = errorAsync.await()
      if (error != null) throw error
    }
  }
}
