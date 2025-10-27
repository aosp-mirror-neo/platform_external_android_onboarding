package com.android.onboarding.tasks;

import android.annotation.SuppressLint;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleCoroutineScopeImpl;
import kotlin.coroutines.CoroutineContext;

/** A java provider for {@link LifecycleCoroutineScopeImpl} to work around internal visibility. */
@SuppressLint("KotlinInternal")
final class OnboardingTaskCoroutineScopeProvider {
  private OnboardingTaskCoroutineScopeProvider() {}

  static LifecycleCoroutineScopeImpl get(Lifecycle lifecycle, CoroutineContext coroutineContext) {
    return new LifecycleCoroutineScopeImpl(lifecycle, coroutineContext);
  }

  static void register(LifecycleCoroutineScopeImpl self) {
    self.register();
  }
}
