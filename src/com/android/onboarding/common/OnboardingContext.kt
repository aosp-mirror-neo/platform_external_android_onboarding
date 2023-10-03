package com.android.onboarding.common

import javax.inject.Qualifier
import kotlin.annotation.Retention

/**
 * Annotation for requesting a dedicated Context for onboarding components. Most of the time this
 * will be an ApplicationContext.
 */
@Qualifier @Retention(AnnotationRetention.RUNTIME) annotation class OnboardingContext
