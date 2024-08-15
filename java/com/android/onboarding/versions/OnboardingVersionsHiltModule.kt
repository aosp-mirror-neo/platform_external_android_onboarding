package com.android.onboarding.versions

import android.content.Context
import com.google.android.apps.common.inject.annotation.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object OnboardingVersionsHiltModule {

  @Provides
  @Reusable
  fun provideOnboardingChanges(@ApplicationContext context: Context): OnboardingChanges =
    DefaultOnboardingChanges(context)
}
