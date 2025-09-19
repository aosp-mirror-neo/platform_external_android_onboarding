package com.android.onboarding.nodes

import java.util.UUID
import javax.inject.Qualifier

typealias NodeId = Long

/**
 * A DI qualifier to inject node ID where applicable. Consuming module should scope this to a given
 * activity's lifecycle.
 */
@Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class OnboardingNodeId

/** Marks entities that require awareness of onboarding node currently being executed. */
interface NodeAware {
  /** A unique id assigned to this node. */
  @OnboardingNodeId val nodeId: NodeId
}

/** Extra key used when storing a node id */
const val EXTRA_ONBOARDING_NODE_ID = "com.android.onboarding.ONBOARDING_NODE_ID"

const val UNKNOWN_NODE_ID: NodeId = -1L

/** Generates a new [NodeId]. */
fun generateNodeId(): NodeId = UUID.randomUUID().leastSignificantBits
