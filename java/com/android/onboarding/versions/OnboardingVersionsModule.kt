package com.android.onboarding.versions

import android.content.Context
import com.google.android.apps.common.inject.annotation.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
internal object OnboardingVersionsModule {

  @Provides
  @Reusable
  fun provideOnboardingChanges(@ApplicationContext context: Context): OnboardingChanges =
    DefaultOnboardingChanges(context)
}
