package com.android.onboarding.common.annotations

/**
 * Annotation used for storing metadata about stable environment reasons.
 *
 * @param name The human readable component name in English
 * @param component The onboarding component that the reason is associated with
 */
@Retention(AnnotationRetention.SOURCE)
annotation class StableEnvironmentReason(val name: String, val component: String)
