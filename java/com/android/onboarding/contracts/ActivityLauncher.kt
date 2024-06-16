package com.android.onboarding.contracts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import com.google.errorprone.annotations.CanIgnoreReturnValue

/** A wrapper around a request to launch a particular activity via a contract. */
open class ActivityLauncher<I>
internal constructor(protected val context: Context, protected open val contract: Launchable<I>) {
  /** Prepares an intent from supplied [args] with the intention of launching it immediately. */
  protected open fun prepareIntent(args: I): Intent =
    contract.launcher.createIntentDirectly(context, args)

  @CanIgnoreReturnValue
  fun launch(args: I): NodeId {
    val intent = prepareIntent(args)
    context.startActivity(intent)
    return intent.getLongExtra(EXTRA_ONBOARDING_NODE_ID, UNKNOWN_NODE_ID)
  }

  /** Launches an activity with given [flags] and contract arguments [args]. */
  @CanIgnoreReturnValue
  fun launch(args: I, flags: Int): NodeId {
    val intent = prepareIntent(args).apply { this.flags = flags }
    context.startActivity(intent)
    return intent.getLongExtra(EXTRA_ONBOARDING_NODE_ID, UNKNOWN_NODE_ID)
  }

  @CanIgnoreReturnValue
  fun launch(args: I, options: Bundle?): NodeId {
    val intent = prepareIntent(args)
    context.startActivity(intent, options)
    return intent.getLongExtra(EXTRA_ONBOARDING_NODE_ID, UNKNOWN_NODE_ID)
  }
}

/** Equivalent to [ComponentActivity.registerForActivityResult] when a result is not expected. */
fun <I> Context.registerForActivityLaunch(contract: Launchable<I>) =
  ActivityLauncher(this, contract)

/** Wrapper over [ComponentActivity.registerForActivityResult] for [LaunchableForResult]. */
fun <I, O> ComponentActivity.registerForActivityResult(
  contract: LaunchableForResult<I, O>,
  callback: ActivityResultCallback<O>,
): ActivityResultLauncher<I> =
  registerForActivityResult(contract.launcher.toActivityResultContract(), callback)
