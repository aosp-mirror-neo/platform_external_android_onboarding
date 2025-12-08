package com.android.onboarding.utils

import android.content.Context
import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A lazy initialised value requiring [I] input to build.
 *
 * The [builder] lambda will be invoked exactly once and the computed value will be cached for all
 * future access.
 */
open class LazyBuilder<I, V>(private val builder: (I) -> V) : ReadOnlyProperty<I, V> {
  private val ref = AtomicReference<V>()

  /** Get or build and cache the value with [input]. */
  @JvmName("get") operator fun invoke(input: I): V = invoke { input }

  /** Get or build and cache the value with lazily computed [input]. */
  @JvmName("get")
  operator fun invoke(input: () -> I): V = ref.updateAndGet { it ?: builder(input()) }

  override fun getValue(thisRef: I, property: KProperty<*>): V = invoke(thisRef)

  /** Get cached value or null if [builder] was not invoked yet. */
  protected val value: V?
    get() = ref.get()
}

/** A [LazyBuilder] which takes [Context] as an argument. */
typealias LazyContextualBuilder<V> = LazyBuilder<Context, V>
