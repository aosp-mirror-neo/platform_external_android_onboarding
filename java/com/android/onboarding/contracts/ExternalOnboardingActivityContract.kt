package com.android.onboarding.contracts

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.android.onboarding.common.EXTERNAL
import com.android.onboarding.contracts.annotations.ForegroundOnboardingNode
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.nodes.NodeId
import com.android.onboarding.pending.ActivityLauncherForResult
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.utils.hashStringToHex
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.PersistableIntent

/**
 * A contract representing off-graph nodes in the onboarding graph. It does not expect to be
 * implemented on the receiving node and should only be used for off-graph launches.
 *
 * Anyone in Onboarding who wants to launch something (for result or not) that doesn't have its own
 * contract should make use of this [ExternalOnboardingActivityContract] - passing in the intent
 * they would have used for a normal [android.app.Activity.startActivity] call - and this start will
 * appear in the graph as a node named after the [Intent.getAction] used for the launch.
 *
 * If it was started for result, then the graph will show the "return" correctly too - and
 * everything that takes place off-graph will be represented as a single "off-graph" node between
 * those two points.
 *
 * If it was started not for result then there will be no "return" events spawned on the graph,
 * however the regular launch sequence events will still be generated.
 */
@OnboardingNode(
  name = "External",
  component = EXTERNAL,
  foreground = ForegroundOnboardingNode(uiType = ForegroundOnboardingNode.UiType.OTHER),
)
object ExternalOnboardingActivityContract :
  OnboardingActivityApiContract<Intent, ActivityResult>() {
  override fun performCreateIntent(context: Context, arg: Intent): Intent = arg

  override fun performExtractArgument(intent: Intent): Intent = intent

  override fun performParseResult(result: ActivityResult): ActivityResult = result

  override fun performSetResult(result: ActivityResult): ActivityResult = result

  @InternalOnboardingApi
  override fun launcher(input: Intent): ActivityLauncherForResult<Intent, ActivityResult> {
    Log.v(
      "ExternalOnboardingActivityContract",
      "Building an external contract launcher for Intent(" +
        "action=${input.action}," +
        "package=${input.`package`}" +
        ")",
    )
    val realLauncher = super.launcher(input)
    return object :
      ActivityLauncherForResult<Intent, ActivityResult>("ExternalOnboardingActivityContract") {
      override val nodeName: String
        get() = input.action?.let(::hashStringToHex) ?: realLauncher.nodeName

      override val nodeComponent: String by realLauncher::nodeComponent

      override fun provideResult(result: ActivityResult): ActivityResult =
        realLauncher.provideResult(result)

      override fun provideSynchronousResult(context: Context, args: Intent) =
        realLauncher.provideSynchronousResult(context, args)

      override fun provideIntent(context: Context, input: Intent) =
        realLauncher.provideIntent(context, input)

      override fun onPrepareIntent(expectsResult: Boolean, nodeId: NodeId, outgoingId: NodeId) =
        realLauncher.onPrepareIntent(expectsResult, nodeId, outgoingId)

      override fun provideEventArgument(input: Intent): Any? =
        realLauncher.provideEventArgument(input)

      override var forResultOutGoingId: NodeId by realLauncher::forResultOutGoingId

      override fun provideEventResult(result: ActivityResult): Any? =
        realLauncher.provideEventResult(result)
    }
  }

  /** @see OnboardingActivityApiContract.prepare */
  fun prepare(context: Context, input: PersistableIntent): PendingContract.ErasedActivity =
    prepare(context, input.toIntent())
}

/** Extract [Intent] from the [contract] and launch it. */
@OptIn(InternalOnboardingApi::class)
fun ActivityResultLauncher<Intent>.launch(context: Context, contract: PendingContract.Activity<*>) =
  launch(contract.createIntent(context))
