@file:OptIn(InternalOnboardingApi::class)

package com.android.onboarding.common

import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.nodes.ComponentRef

/** A [ComponentRef] of [TEST_APP]. */
@JvmField val TEST_APP_REF = ComponentRef(TEST_APP)

/** A [ComponentRef] of [TEST_APP_2]. */
@JvmField val TEST_APP_2_REF = ComponentRef(TEST_APP_2)

/** A [ComponentRef] of [MANAGED_PROVISIONING]. */
@JvmField val MANAGED_PROVISIONING_REF = ComponentRef(MANAGED_PROVISIONING)
