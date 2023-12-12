package com.android.onboarding.versions.annotations

/**
 * Marker for a change made to an Onboarding component.
 *
 * All changes in Onboarding must be marked by a [ChangeId] and fully flagged.
 *
 * The value should be the ID of a Buganizer issue related to the change.
 *
 * @param owner A username who is responsible for this change
 * @param breaking Reasons that this change will cause breakages in current callers. Empty if none.
 * @param changeRadius The radius of impact of this change. See [ChangeRadius] docs for details.
 * @param dependsOn Another Change ID that must be enabled for this change to work. [DEFAULT] for
 *   none.
 * @param expedited A reason that this change may skip the usual rollout process.
 *   [ExpeditedReason.NOT_EXPEDITED] if none.
 */
@Target(AnnotationTarget.FIELD)
annotation class ChangeId(
  val owner: String,
  val breaking: Array<BreakingReason>,
  val changeRadius: ChangeRadius,
  val dependsOn: Long = DEFAULT,
  val expedited: ExpeditedReason = ExpeditedReason.NOT_EXPEDITED
) {
  companion object {
    private const val DEFAULT = -1L
  }
}
