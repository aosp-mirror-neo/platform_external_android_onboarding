package com.android.onboarding.control

import android.os.PersistableBundle
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableBundle
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable

/**
 * A container around the data needed to launch an activity from an orchestrator.
 *
 * If [requestCode] is non-null then we expect this to be launched forResult.
 */
data class ActivityLaunch(
  val contract: PendingContract.ErasedActivity,
  val requestCode: Int? = null,
) : Persistable, StableEquality {
  override fun toPersistableBundle() = PersistableBundle {
    putPersistable(KEY_CONTRACT, contract)
    requestCode?.let { putInt(KEY_REQUEST_CODE, it) }
  }

  companion object : PersistableDeserializer<ActivityLaunch> {
    private const val KEY_CONTRACT = "contract"
    private const val KEY_REQUEST_CODE = "requestCode"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): ActivityLaunch =
      ActivityLaunch(
        contract =
          requireNotNull(persistableBundle.getPersistable(KEY_CONTRACT, PendingContract.Activity)),
        requestCode =
          if (persistableBundle.containsKey(KEY_REQUEST_CODE))
            persistableBundle.getInt(KEY_REQUEST_CODE)
          else null,
      )
  }
}
