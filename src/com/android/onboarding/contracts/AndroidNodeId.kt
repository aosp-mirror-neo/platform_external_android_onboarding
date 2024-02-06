package com.android.onboarding.contracts

import android.app.Activity
import android.content.Intent
import java.util.UUID

val Intent.nodeId: NodeId
  get() =
    getLongExtra(EXTRA_ONBOARDING_NODE_ID, UNKNOWN_NODE_ID).let {
      if (it == UNKNOWN_NODE_ID) {
        val id = UUID.randomUUID().leastSignificantBits
        this.putExtra(EXTRA_ONBOARDING_NODE_ID, id)
        id
      } else {
        it
      }
    }

val Activity.nodeId: NodeId
  get() {
    intent = intent ?: Intent()
    return intent.nodeId
  }
