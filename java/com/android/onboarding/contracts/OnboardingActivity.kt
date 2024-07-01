package com.android.onboarding.contracts

import kotlin.reflect.KClass

/**
 * Marks a given [Activity] as an Onboarding entity fulfilling the given [contract].
 *
 * Readers should see the documentation of [contract] for details on the expectations of callers.
 *
 * @property contract fulfilled by this activity
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class OnboardingActivity(val contract: KClass<out OnboardingActivityApiContract<*, *>>)
