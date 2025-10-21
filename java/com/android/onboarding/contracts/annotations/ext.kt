@file:OptIn(InternalOnboardingApi::class)

package com.android.onboarding.contracts.annotations

import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef

/** Extract [NodeRef] from this [POnboardingNode]. */
fun POnboardingNode.asNodeRef(): NodeRef.Stable = NodeRef(ComponentRef(component), name)
