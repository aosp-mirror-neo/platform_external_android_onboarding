package com.android.onboarding.versions.annotations

/**
 * Annotation used to indicate that the passed value must either be a long constant annotated with
 * [ChangeId] or a variable which itself was annotated [ValidChangeId].
 *
 * @param allowedChangeRadii The change radius allowed for passed change IDs
 * @param disallowedChangeRadiiError The error to use if a change ID with an invalid change radius
 *   is provided. This should ideally tell the developer what to do.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class ValidChangeId(
  val allowedChangeRadii: Array<ChangeRadius> =
    [ChangeRadius.SINGLE_COMPONENT, ChangeRadius.MULTI_COMPONENT],
  val disallowedChangeRadiiError: String =
    "The provided change ID does not have a valid change radius.",
)
