package com.android.onboarding.contracts

import javax.inject.Qualifier

typealias NodeId = Long

/**
 * A DI qualifier to inject node ID where applicable. Consuming module should scope this to a given
 * activity's lifecycle.
 */
@Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class OnboardingNodeId

/**
 * Marks entities that require awareness of onboarding node currently being executed
 *
 * @property nodeId the ID of the currently executing node
 */
interface NodeAware {
  @OnboardingNodeId val nodeId: NodeId
}

/** Extra key used when storing a node id */
const val EXTRA_ONBOARDING_NODE_ID = "com.android.onboarding.ONBOARDING_NODE_ID"

const val UNKNOWN_NODE_ID: NodeId = -1L
