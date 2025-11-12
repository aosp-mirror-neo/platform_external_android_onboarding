package com.android.onboarding.tasks.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.preferencesDataStore
import com.android.onboarding.contracts.annotations.InternalOnboardingApi

// Note: This must not collide with other DataStore file names within an app.

private const val DATA_STORE_NAME = "OnboardingTaskDataStore"


/**
 * A singleton [DataStore] that stores data related to the onboarding task manager infrastructure.
 *
 * This must be a singleton for a file name and process.
 */
@InternalOnboardingApi
val Context.taskDataStore: DataStore<Preferences> by
  preferencesDataStore(
    name = DATA_STORE_NAME,
    corruptionHandler = ReplaceFileCorruptionHandler { mutablePreferencesOf() },
  )

@InternalOnboardingApi const val DATA_STORE_KEY_PREFIX = "OnboardingTaskInfra"
