package com.android.onboarding.flags

import dagger.Binds
import dagger.Module
import dagger.Reusable

@Module
interface OnboardingFlagsModule {
  @Binds
  @Reusable
  fun provideOnboardingFlagsProvider(impl: DefaultOnboardingFlagsProvider): OnboardingFlagsProvider
}
