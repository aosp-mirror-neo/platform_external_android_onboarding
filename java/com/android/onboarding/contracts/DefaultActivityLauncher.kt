package com.android.onboarding.contracts

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.EXTRA_ONBOARDING_NODE_ID
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.nodes.UNKNOWN_NODE_ID
import com.android.onboarding.pending.Launchable
import com.android.onboarding.pending.LaunchableActivityForResult
import com.google.errorprone.annotations.CanIgnoreReturnValue

/** [ActivityLauncher] to use from inside existing Activities. */
@OptIn(InternalOnboardingApi::class)
open class DefaultActivityLauncher<I>
internal constructor(protected val context: Context, protected open val contract: Launchable<I>) :
  ActivityLauncher<I> {
  /** Prepares an intent from supplied [args] with the intention of launching it immediately. */
  protected open fun prepareIntent(args: I): Intent =
    contract.launcher(args).createIntentDirectly(context, args)

  @CanIgnoreReturnValue
  override fun launch(input: I, flags: Int?, options: Bundle?): NodeId {
    val intent =
      prepareIntent(input).apply {
        if (flags != null) {
          this.flags = flags
        }
      }
    val targetUserId = contract.getTargetUserId(input)
    if (targetUserId != UserHandle.myUserId()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.startActivityAsUser(intent, UserHandle.of(targetUserId))
      } else {
        throw UnsupportedOperationException(
          "Cross user activity launch is not supported on this platform, requires U+"
        )
      }
    } else {
      context.startActivity(intent, options)
    }
    return intent.getLongExtra(EXTRA_ONBOARDING_NODE_ID, UNKNOWN_NODE_ID)
  }

  companion object {
    private const val TAG = "DefaultActivityLauncher"
  }
}

/** Equivalent to [ComponentActivity.registerForActivityResult] when a result is not expected. */
fun <I> Context.registerForActivityLaunch(contract: Launchable<I>): ActivityLauncher<I> =
  DefaultActivityLauncher(this, contract)

/** Wrapper over [ComponentActivity.registerForActivityResult] for [LaunchableActivityForResult]. */
@OptIn(InternalOnboardingApi::class)
fun <I, O> ComponentActivity.registerForActivityResult(
  contract: LaunchableActivityForResult<I, O>,
  callback: ActivityResultCallback<O>,
): ActivityResultLauncher<I> =
  registerForActivityResult(contract.toActivityResultContract(), callback)
