package com.android.onboarding.pending

import android.content.Context.BIND_ALLOW_ACTIVITY_STARTS
import android.os.Build
import android.os.PersistableBundle
import android.os.Process.myUserHandle
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.encoding.ResultHandler
import com.android.onboarding.encoding.WithInputEncoder
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.nodes.generateHash
import com.android.onboarding.utils.generateHash
import com.android.onboarding.utils.logging.OnboardingLog
import com.android.onboarding.utils.persistable.PersistableIntent
import java.util.Objects

/**
 * Represents an entity which can be launched using Onboarding background tasks with an input of
 * type [I].
 */
interface BackgroundLaunchable<I> : NodeRef, WithInputEncoder<I, PersistableBundle> {
  /**
   * Returns the intent which connects to the corresponding service to run the
   * [OnboardingTask][com.android.onboarding.tasks.OnboardingTask].
   *
   * It is expected that implementing component will have registered an intent filter for such
   * action linking to the onboarding task manager service.
   *
   * Special rules apply to GMS components. See ___ for more details.
   */
  val taskServiceIntent: PersistableIntent

  /** Returns the target user id for the task, given the [input]. */
  fun getTargetUserId(input: I): Int {
    return myUserHandle().identifier
  }

  /** Returns the target component for the task, given the [input]. */
  fun getTargetComponent(input: I): String {
    return nodeComponent
  }

  companion object {
    private val log = OnboardingLog.logger("BackgroundLaunchable")
    const val SERVICE_ACTION = "com.android.onboarding.task.RUN_ONBOARDING_TASK"

    /**
     * Prepared an intent to bind to a service for a given [component].
     *
     * Handles GMS special-casing to convert standard `action + category` routing into unique
     * `action={action}/{category}` action-only routing. Due to historic reasons, "auth" gms
     * subcomponent is the only one not special-cased.
     *
     * See ___ for more details.
     */
    @InternalOnboardingApi
    fun prepareBindingIntent(component: ComponentRef): PersistableIntent {
      val intent =
        PersistableIntent(
          action = SERVICE_ACTION,
          `package` = component.nodePackage,
          categories = setOf(component.subcomponent),
        )
      if (component.identity() == "com.google.android.apps.restore/restore") {
        if (Build.VERSION.SDK_INT >= 34) {
          log.v("Allowing activity starts as special case for restore")
          intent.addFlags(BIND_ALLOW_ACTIVITY_STARTS)
        }
      }
      if (component.nodePackage == "com.google.android.gms" && component.subcomponent != "auth") {
        log.v("Rewriting GMS intent action")
        // Clear categories and rewrite GMS intent action so it's unique for all submodules.
        intent.categories = emptySet()
        intent.action += "/${component.subcomponent}"
        log.v("Rewrote GMS intent action to ${intent.action}")
      }
      return intent
    }
  }
}

/**
 * Represents an entity which can be launched using Onboarding background tasks with an input of
 * type [I] and an output of type [O].
 */
interface BackgroundLaunchableForResult<I, O> :
  BackgroundLaunchable<I>, ResultHandler<PersistableBundle, O> {
  /** Generates a hash for the combination of this entity and the provided [input]. */
  fun generateHash(input: I): Int =
    Objects.hash((this as NodeRef).generateHash(), inputEncoder.encode(input).generateHash())
}
