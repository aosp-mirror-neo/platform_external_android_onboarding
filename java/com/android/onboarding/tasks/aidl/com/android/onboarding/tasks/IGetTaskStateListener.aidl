package com.android.onboarding.tasks;

import com.android.onboarding.tasks.ErasedOnboardingTaskState;

/**
 * A listener for getting the state of onboarding tasks.
 */
interface IGetTaskStateListener {
    /**
     * Callback when a task state is obtained.
     *
     * @param state erased state for the task.
     */
    void onTaskStateObtained(in ErasedOnboardingTaskState state) = 0;
}
