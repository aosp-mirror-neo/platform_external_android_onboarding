package com.android.onboarding.versions.changes

import com.android.onboarding.versions.annotations.ChangeId
import com.android.onboarding.versions.annotations.ChangeRadius
import com.android.onboarding.versions.changes.Tags.NEXT
import com.android.onboarding.versions.changes.Tags.P10_DVT
import com.android.onboarding.versions.changes.Tags.P10_PVT
import com.android.onboarding.versions.changes.Tags.P11_EVT

@ChangeId(
  bugId = 383718037L,
  owner = "petuska",
  breaking = [],
  available = "2025-02-01",
  released = "2025-12-05",
  tags = [P10_DVT],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
)
const val SUB_ORCHESTRATION_CHANGE_ID = 383718037L

/**
 * Enabled UWS invisibility control tied to orchestrated task's `estimatedCallbackRuntime` values.
 */
@ChangeId(
  bugId = 408958992L,
  owner = "petuska",
  breaking = [],
  available = "2025-02-25",
  tags = [P10_PVT],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
)
const val ORCHESTRATED_VISIBILITY_CHANGE_ID = 408958992L

@ChangeId(
  bugId = 404239383,
  owner = "eliselliott",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = [P11_EVT, NEXT],
  available = "2025-12-25",
)
const val USE_ORCHESTRATOR_CHANGE_ID = 404239383L
