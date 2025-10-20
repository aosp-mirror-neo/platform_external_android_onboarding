package com.android.onboarding.nodes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.BaseBundle

/**
 * Extract a [NodeId] from this [Context] making an assumption that this is an [Activity].
 *
 * This should only be called on an [Activity].
 */
fun Context.activityNodeId(): NodeId =
  // This is true in all known cases - but we need to accept Context because that's the AndroidX API
  // surface
  if (this is Activity) {
    nodeId
  } else {

    UNKNOWN_NODE_ID
  }

/** Mark the executing node as failed with [reason]. */
fun Activity.failNode(reason: String? = null) {
  OnboardingGraphLog.log(OnboardingEvent.ActivityNodeFail(nodeId, reason))
}

private inline fun NodeId?.verify(setter: (NodeId) -> Unit): NodeId {
  return if (this == null || this == UNKNOWN_NODE_ID) {
    generateNodeId().also(setter)
  } else {
    this
  }
}

var BaseBundle.nodeId: NodeId
  get() =
    getLong(EXTRA_ONBOARDING_NODE_ID, UNKNOWN_NODE_ID).verify { value ->
      putLong(EXTRA_ONBOARDING_NODE_ID, value)
    }
  set(value) {
    putLong(EXTRA_ONBOARDING_NODE_ID, value)
  }

var Intent.nodeId: NodeId
  get() =
    getLongExtra(EXTRA_ONBOARDING_NODE_ID, UNKNOWN_NODE_ID).verify { value ->
      putExtra(EXTRA_ONBOARDING_NODE_ID, value)
    }
  set(value) {
    putExtra(EXTRA_ONBOARDING_NODE_ID, value)
  }

val Activity.nodeId: NodeId
  get() {
    intent = intent ?: Intent()
    return intent.nodeId
  }
