package com.android.onboarding.tasks

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a token to an onboarding task executor along with its associated contract.
 *
 * This class is used to uniquely identify and reference an onboarding task executor and its
 * contract.
 */
@Parcelize
data class OnboardingTaskToken(val taskClass: String, val taskContractClass: String) : Parcelable
