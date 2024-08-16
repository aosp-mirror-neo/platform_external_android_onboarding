package com.android.onboarding.contracts.testing

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.R

/**
 * An Activity to be used in tests. It can be created with Robolectric, and makes it possible to use
 * a generic Fragment container for the tests.
 */
class TestAppCompatActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setTheme(R.style.Base_Theme_AppCompat)
  }
}
