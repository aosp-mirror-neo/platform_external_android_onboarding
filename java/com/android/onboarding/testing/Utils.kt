package com.android.onboarding.testing

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import org.apache.commons.lang3.ClassUtils

/**
 * Compares that [lhs] is equal to [rhs] via reflection by recursively checking that all kotlin
 * properties are equal. The usage of this function should only be reserved for cases where the
 * objects being checked cannot guarantee a reliable [Any.equals] implementation.
 *
 * Behaviour for Java objects is undefined.
 */
fun recursiveReflectionEquals(lhs: Any?, rhs: Any?): Boolean {
  if (lhs == null || rhs == null) return lhs == rhs
  if (lhs == rhs) return true
  if (
    ClassUtils.isPrimitiveOrWrapper(lhs::class.java) ||
      ClassUtils.isPrimitiveOrWrapper(rhs::class.java)
  ) {
    return lhs == rhs
  }
  if (isPrimitiveArray(lhs) && isPrimitiveArray(rhs)) {
    return arrayOf(lhs).contentDeepEquals(arrayOf(rhs))
  }
  if (lhs is Array<*> && rhs is Array<*>) {
    return lhs.zip(rhs).none { (l, r) -> !recursiveReflectionEquals(l, r) }
  }
  if (lhs is Iterable<*> && rhs is Iterable<*>) {
    return lhs.zip(rhs).none { (l, r) -> !recursiveReflectionEquals(l, r) }
  }
  val leftProps = lhs::class.memberProperties.associateBy(KProperty1<*, *>::name)
  val rightProps = rhs::class.memberProperties.associateBy(KProperty1<*, *>::name)
  @Suppress("UNCHECKED_CAST")
  return leftProps.none search@{ (lName, lProp) ->
    val rProp = rightProps[lName] ?: return@search false
    val lValue =
      (lProp as KProperty1<Any, *>).run {
        val accessible = isAccessible
        isAccessible = true
        val value = get(lhs)
        isAccessible = accessible
        value
      }
    val rValue =
      (rProp as KProperty1<Any, *>).run {
        val accessible = isAccessible
        isAccessible = true
        val value = get(rhs)
        isAccessible = accessible
        value
      }
    !recursiveReflectionEquals(lValue, rValue)
  }
}

private fun isPrimitiveArray(obj: Any): Boolean =
  obj is ByteArray ||
    obj is CharArray ||
    obj is ShortArray ||
    obj is IntArray ||
    obj is LongArray ||
    obj is DoubleArray ||
    obj is FloatArray ||
    obj is BooleanArray
