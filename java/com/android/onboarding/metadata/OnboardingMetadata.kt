package com.android.onboarding.metadata

import android.content.Context
import com.android.onboarding.utils.persistable.Persistable

/**
 * Implemented by "metadata interfaces".
 *
 * A metadata interface can be specified in the "metadata" attribute of the OnboardingNode
 * annotation. This makes it possible to use the `updateMetadata` methods to pass typed metadata
 * which can be read by other onboarding components.
 *
 * Subclasses should be annotated with
 * [UniqueKey][com.android.onboarding.common.annotations.UniqueKey].
 */
interface OnboardingMetadata : Persistable {

  /**
   * The preferred display of a metadata as a string, which will fully resolve content where
   * possible.
   */
  @Suppress("DEPRECATION") fun toString(context: Context?) = toString()

  @Deprecated("Prefer calling the version which takes a Context which allows for resolving strings")
  override fun toString(): String
}
