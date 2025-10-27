package com.android.onboarding.tasks;

import com.android.onboarding.tasks.ErasedOnboardingTaskState;

/**
 * A listener for onboarding tasks.
 */
interface IOnboardingTaskManagerServiceListener {
    /**
     * Callback when a task completes.
     *
     * @param state erased state for the task.
     * @param metadata the latest metadata emitted before the task completes.
     */
    void onTaskCompleted(in ErasedOnboardingTaskState state, in PersistableBundle metadata) = 0;

    /**
     * Callback when a task metadata updates.
     *
     * @param metadata the updated metadata.
     */
    void onTaskMetadataUpdated(in PersistableBundle metadata) = 1;
}