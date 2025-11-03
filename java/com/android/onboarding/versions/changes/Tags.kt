package com.android.onboarding.versions.changes

/** Tags that can be used on [com.android.onboarding.versions.annotations.ChangeId.tags] */
object Tags {
  const val P10_DVT = "p10_dvt"
  const val P10_PVT = "p10_pvt"
  const val P11_EVT = "p11_evt"
  const val P11_DVT = "p11_dvt"
  const val P11_PVT = "p11_pvt"

  /**
   * Tag for all unreleased changes.
   *
   * When using any of the tags above, [NEXT] should also be included for to make it a superset of
   * all tags.
   */
  const val NEXT = "next"
}
