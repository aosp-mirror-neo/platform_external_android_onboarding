package com.android.onboarding.common.annotations

/**
 * Annotation used for storing metadata about Onboarding components.
 *
 * @param name The human readable component name in English
 * @param packageName The Android package name of the component
 * @param subName If packageName does not uniquely identify this component, this is an additional
 *   short string to differentiate from others in the same package
 */
@Retention(AnnotationRetention.SOURCE)
annotation class OnboardingComponent(
  val name: String,
  val packageName: String,
  val subName: String = "",
)
