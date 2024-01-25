package com.android.onboarding.bedsteadonboarding.annotations

import kotlin.reflect.KClass

/**
 * Mark that the test will allow the given node to be executed.
 *
 * @property contract the Kclass of the contract for which the nodes are allowed to execute.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestSingleNode(val contract: KClass<*>)
