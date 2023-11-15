package com.android.onboarding.flags

import com.google.android.gms.pommel.InstallIn
import com.google.android.gms.pommel.components.GmsModuleComponent
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
@InstallIn(GmsModuleComponent::class)
object OnboardingFlagsPommelModule {

  @Provides
  @Reusable
  fun provideOnboardingFlagsProvider(): OnboardingFlagsProvider = DefaultOnboardingFlagsProvider()
}
