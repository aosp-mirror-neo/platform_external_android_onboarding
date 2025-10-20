package com.android.onboarding.nodes

import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.OnboardingNode

/** Get the [NodeRef] for the given [node]. */
@OptIn(InternalOnboardingApi::class)
fun OnboardingNode.Companion.nodeRef(node: OnboardingNode): NodeRef =
  NodeRef(ComponentRef(node.component), node.name)

/**
 * Get the [NodeRef] for the given [nodeClass].
 *
 * This will throw an exception if it is not a correctly annotated node class.
 */
@Throws(java.lang.IllegalArgumentException::class)
fun OnboardingNode.Companion.nodeRef(nodeClass: Class<*>): NodeRef =
  nodeRefOrNull(nodeClass)
    ?: throw IllegalArgumentException("nodeClass must be correctly annotated")

/** Get the [NodeRef] for the given node class. */
fun OnboardingNode.Companion.nodeRefOrNull(nodeClass: Class<*>): NodeRef? =
  extractNodeMetadataOrNull(nodeClass)?.let(::nodeRef)
