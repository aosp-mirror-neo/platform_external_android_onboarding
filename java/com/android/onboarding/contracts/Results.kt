package com.android.onboarding.contracts

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult

/** A result returned by an onboarding node. */
sealed interface NodeResult {
  interface Success : NodeResult

  interface Failure : NodeResult
}

/* A result returned by an [Activity] node. */
interface ActivityNodeResult {
  fun toActivityResult(): ActivityResult
}

/** A result that is common to all activity nodes. */
sealed interface CommonActivityNodeResult : ActivityNodeResult {
  /** User navigated back. */
  data object Back : CommonActivityNodeResult, NodeResult.Success {
    override fun toActivityResult() =
      ActivityResult(
        Activity.RESULT_CANCELED,
        Intent().apply { putExtra(EXTRA_BACK_PRESSED, true) },
      )
  }

  /**
   * The process hosting the activity has crashed.
   *
   * Marked as Success until all nodes rollout ___ to avoid misleading people.
   */
  data object Crash : CommonActivityNodeResult, NodeResult.Success {
    override fun toActivityResult() = ActivityResult(Activity.RESULT_CANCELED, data = null)
  }

  companion object {
    const val EXTRA_BACK_PRESSED = "com.android.onboarding.EXTRA_BACK_PRESSED"
  }
}

internal object CommonActivityNodeResultFactory {
  fun create(activityResult: ActivityResult): CommonActivityNodeResult =
    if (activityResult.resultCode == Activity.RESULT_CANCELED && activityResult.data == null)
      CommonActivityNodeResult.Crash
    else if (
      activityResult.resultCode == Activity.RESULT_CANCELED &&
        activityResult.data?.getBooleanExtra(CommonActivityNodeResult.EXTRA_BACK_PRESSED, false) ==
          true
    )
      CommonActivityNodeResult.Back
    else
      throw IllegalArgumentException(
        "activity result: $activityResult is not a common activity node result"
      )
}
