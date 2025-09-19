package com.android.onboarding.testing

import com.google.common.truth.Fact
import com.google.common.truth.Subject

/**
 * Groups multiple assertions on the same subject.
 *
 * ```kotlin
 * assertThat(69) satisfiesAll {
 *   isNotNull()
 *   isGreaterThan(68)
 * }
 * ```
 *
 * @see rangeTo
 */
inline infix fun <T : Subject> T.satisfiesAll(assertions: T.() -> Unit) = assertions()

/**
 * Nicer version of [satisfiesAll].
 *
 * ```kotlin
 * assertThat(69)..{
 *   isNotNull()
 *   isGreaterThan(68)
 * }
 * ```
 *
 * @see satisfiesAll
 */
inline operator fun <T : Subject> T.rangeTo(assertions: T.() -> Unit) = satisfiesAll(assertions)

// Define these locally in case Truth ever gets reimplemented in Kotlin. Then imports would have to
// change from import c.g.c.truth.Fact.fact to import c.g.c.truth.Fact.Companion.fact.

internal fun fact(key: String, value: Any?) = Fact.fact(key, value)

internal fun simpleFact(fact: String) = Fact.simpleFact(fact)
