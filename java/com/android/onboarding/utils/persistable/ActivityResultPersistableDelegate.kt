package com.android.onboarding.utils.persistable

import android.os.PersistableBundle
import androidx.activity.result.ActivityResult

/**
 * A [PersistableDelegate] for [ActivityResult] to make it fit the [Persistable] interface.
 *
 * Note that it uses [PersistableIntent] to serialise [ActivityResult.data] and as such is subject
 * to all of its limitations.
 */
object ActivityResultPersistableDelegate : PersistableDelegate<ActivityResult> {
  private const val KEY_RESULT_CODE = "par.resultCode"
  private const val KEY_DATA = "par.data"

  override fun toPersistableBundle(value: ActivityResult): PersistableBundle =
    PersistableBundle().apply {
      putInt(KEY_RESULT_CODE, value.resultCode)
      value.data?.let { putPersistable(KEY_DATA, PersistableIntent(it)) }
    }

  override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
    ActivityResult(
      resultCode = persistableBundle.getInt(KEY_RESULT_CODE),
      data = persistableBundle.getPersistable(KEY_DATA, PersistableIntent)?.toIntent(),
    )

  /**
   * Returns [true] if the [persistableBundle] is the product of an [ActivityResult] serialization.
   */
  fun isActivityResultPersistable(persistableBundle: PersistableBundle): Boolean {
    return persistableBundle.containsKey(KEY_RESULT_CODE)
  }
}
