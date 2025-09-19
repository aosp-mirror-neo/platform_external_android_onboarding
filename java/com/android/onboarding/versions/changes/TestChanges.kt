package com.android.onboarding.versions.changes

import com.android.onboarding.versions.annotations.ChangeId
import com.android.onboarding.versions.annotations.ChangeRadius

// Changes in this file are for use by the Onboarding Versions test suite

@ChangeId(
  bugId = 1234567L,
  owner = "anowner",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  tags = ["UNAVAILABLE_CHANGE_ID_TAG"],
)
const val UNAVAILABLE_CHANGE_ID = 1234567L

@ChangeId(
  bugId = 2345678L,
  owner = "anowner",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  available = "2024-01-02",
)
const val AVAILABLE_2024_01_02_CHANGE_ID = 2345678L

@ChangeId(
  bugId = 3456789L,
  owner = "anowner",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
  available = "2024-01-02",
  released = "2024-02-02",
)
const val AVAILABLE_2024_01_02_RELEASED_2024_02_02_CHANGE_ID = 3456789L

@ChangeId(
  bugId = 111111L,
  owner = "anowner",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
  available = "2024-01-02",
  released = "2024-02-02",
)
const val SINGLE_COMPONENT_AVAILABLE_2024_01_02_RELEASED_2024_02_02_CHANGE_ID = 111111L

// This change should never be made available. This is depended on in production.
@ChangeId(
  bugId = 111112L,
  owner = "anowner",
  breaking = [],
  changeRadius = ChangeRadius.MULTI_COMPONENT,
)
const val NEVER_AVAILABLE_MULTI_COMPONENT_CHANGE = 111112L

// This change should never be made available. This is depended on in production.
@ChangeId(
  bugId = 111113L,
  owner = "anowner",
  breaking = [],
  changeRadius = ChangeRadius.SINGLE_COMPONENT,
)
const val NEVER_AVAILABLE_SINGLE_COMPONENT_CHANGE = 111113L
