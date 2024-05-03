package com.android.onboarding.bedsteadonboarding.annotations

import com.android.onboarding.contracts.OnboardingActivityApiContract
import kotlin.reflect.KClass

/**
 * A reference to a single node definition.
 *
 * @property contract the KClass of the contract to execute the node.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestSingleNode(val contract: KClass<out OnboardingActivityApiContract<*, *>>)
