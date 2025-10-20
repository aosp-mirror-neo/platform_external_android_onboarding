package com.android.onboarding.utils

/**
 * Marker interface indicating that implementations have overridden [equals] and [hashCode].
 *
 * @see StableEquality
 * @see KnownStableEquality
 */
interface OverriddenEquality {
  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int
}

/**
 * Marker interface indicating the guarantees of [equals] and [hashCode] method overrides.
 *
 * @see UnknownEquality
 * @see OverriddenEquality
 * @see StableEquality
 * @see KnownStableEquality
 */
sealed interface Equality : OverriddenEquality

/**
 * Marker interface indicating that implementations have overridden [equals] and [hashCode] but
 * cannot make any stability guarantees and as such those methods should not be relied upon.
 *
 * Main use-case for this is cases where super class demands [OverriddenEquality] and
 * implementations have [hashCode] and [equals] generated (such as data classes), but know that
 * those implementations do not provide [StableEquality] guarantees.
 */
interface UnknownEquality : Equality

/**
 * Marker interface indicating that implementations have overridden [equals] and [hashCode] methods
 * to treat two objects with the same type and content as identical, even when it's not the same
 * instance.
 *
 * @see UnknownEquality
 * @see KnownStableEquality
 */
interface StableEquality : Equality {
  /** Check that [other] is the same type as this object and has equal content. */
  override fun equals(other: Any?): Boolean

  /** Generate a hash of all of this object's contents. */
  override fun hashCode(): Int
}

/**
 * Annotation which can be used to annotate parameters with external types which are known to
 * implicitly have [StableEquality] guarantees.
 *
 * ```kotlin
 * data class MyType(
 *   val str: String,
 *   val known: @KnownStableEquality Uri,
 *   val knownCollection: List<@KnownStableEquality Uri>,
 * ): StableEquality
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.TYPE)
annotation class KnownStableEquality
