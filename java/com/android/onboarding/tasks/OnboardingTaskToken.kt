package com.android.onboarding.tasks

import android.os.Parcel
import android.os.Parcelable

/**
 * Represents a token to an onboarding task executor along with its associated contract.
 *
 * This class is used to uniquely identify and reference an onboarding task executor and its
 * contract.
 *
 * @property taskContractClass The fully qualified class name of the associated
 *   [OnboardingTaskContract].
 * @property taskComponentName The component name of the task. Name is defined in
 *   [OnboardingComponents].
 */
data class OnboardingTaskToken(val taskContractClass: String, val taskComponentName: String) :
  Parcelable {
  constructor(
    parcel: Parcel
  ) : this(
    taskContractClass = parcel.readString() ?: error("taskContractClass is missing in the parcel"),
    taskComponentName = parcel.readString() ?: error("taskComponentName is missing in the parcel"),
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(taskContractClass)
    parcel.writeString(taskComponentName)
  }

  override fun describeContents(): Int = 0

  companion object CREATOR : Parcelable.Creator<OnboardingTaskToken> {
    const val INVALID_STRING = "INVALID"
    // Constant representing an invalid token
    val INVALID: OnboardingTaskToken =
      OnboardingTaskToken(taskContractClass = INVALID_STRING, taskComponentName = INVALID_STRING)

    override fun createFromParcel(parcel: Parcel): OnboardingTaskToken {
      return OnboardingTaskToken(parcel)
    }

    override fun newArray(size: Int): Array<OnboardingTaskToken?> {
      return arrayOfNulls(size)
    }
  }
}
