package com.android.onboarding.control

import android.os.PersistableBundle
import com.android.onboarding.compatibility.compatGetBoolean
import com.android.onboarding.compatibility.compatPutBoolean
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.NullSafePersistableBundleExtensions.getIntOrThrow
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableBundle
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import kotlin.random.Random

/**
 * A container around the data needed to launch an activity from an orchestrator.
 *
 * [isForResult] determines if this launch expects a result.
 */
data class ActivityLaunch(
  val contract: PendingContract.Activity<*>,
  val isForResult: Boolean,
  val requestCode: Int = Random.nextInt(),
) : Persistable, StableEquality, NodeRef by contract {

  override fun toPersistableBundle() = PersistableBundle {
    putPersistable(KEY_CONTRACT, contract.erase())
    compatPutBoolean(KEY_IS_FOR_RESULT, isForResult)
    putInt(KEY_REQUEST_CODE, requestCode)
  }

  companion object : PersistableDeserializer<ActivityLaunch> {
    private const val KEY_CONTRACT = "contract"
    private const val KEY_IS_FOR_RESULT = "is_for_result"
    private const val KEY_REQUEST_CODE = "requestCode"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle): ActivityLaunch =
      ActivityLaunch(
        contract =
          requireNotNull(persistableBundle.getPersistable(KEY_CONTRACT, PendingContract.Activity)),
        isForResult = persistableBundle.compatGetBoolean(KEY_IS_FOR_RESULT),
        requestCode = persistableBundle.getIntOrThrow(KEY_REQUEST_CODE),
      )
  }
}
