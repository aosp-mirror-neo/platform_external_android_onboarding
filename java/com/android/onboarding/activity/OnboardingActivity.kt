package com.android.onboarding.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.ContentView
import androidx.annotation.LayoutRes
import com.android.onboarding.contracts.AttachedResult
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.DiscouragedOnboardingApi
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.contracts.registerForActivityLaunch
import kotlin.properties.Delegates

/** An activity base for onboarding components linking them directly to a given [contract]. */
abstract class OnboardingActivity<I : Any, O : Any, C : OnboardingActivityApiContract<I, O>> :
  AppCompatActivity {
  constructor() : super()

  @ContentView
  constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

  /**
   * [OnboardingActivityApiContract] bound to this activity.
   *
   * If injected lazily, implementations
   * must ensure that it is available before any calls to [onCreate].
   */
  protected abstract val contract: C

  /**
   * Argument extracted for this activity instance via the provided [contract].
   *
   * Accessing it before [onCreate] will result in an exception.
   */
  protected var argument: I by Delegates.notNull()
    private set

  protected var attachResult: AttachedResult by Delegates.notNull()
    private set

  /**
   * Checks [OnboardingNode.specificationType] to determine if internal errors should be eager or
   * lazy.
   */
  private val strict by lazy {
    @OptIn(InternalOnboardingApi::class)
    contract.metadata.specificationType > OnboardingNode.SpecificationType.BASELINE
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    attachResult = contract.attach(this, rawIntent)
    runCatching { contract.extractArgument(rawIntent) }
      .onSuccess { argument = it }
      .onFailure { if (strict) throw it }
  }

  /**
   * Sets the activity [result] via [contract].
   *
   * @param result typesafe representation of the activity result
   */
  protected fun setResult(result: O) {
    contract.setResult(this, result)
  }

  /** Direct access to the [Activity.getIntent] bypassing all the checks. */
  internal inline val rawIntent
    get() = super.getIntent()

  private inline fun <R> callStrictApi(method: String, call: () -> R): R =
    if (strict) {
      error(
        "Calls to ::$method are forbidden for OnboardingNode.specificationType > OnboardingNode.SpecificationType.BASELINE"
      )
    } else {
      call()
    }

  /**
   * Prefer [argument] instead.
   */
  @DiscouragedOnboardingApi("Prefer accessing intent data via extracted argument")
  override fun getIntent(): Intent = callStrictApi("getIntent") { rawIntent }

  /**
   * Prefer [registerForActivityLaunch] instead.
   */
  @DiscouragedOnboardingApi("Prefer launching activities via their contracts")
  override fun startActivity(intent: Intent) =
    callStrictApi("startActivity") { super.startActivity(intent) }

  /**
   * Prefer [registerForActivityLaunch] instead.
   */
  @DiscouragedOnboardingApi("Prefer launching activities via their contracts")
  override fun startActivity(intent: Intent, options: Bundle?) =
    callStrictApi("startActivity") { super.startActivity(intent, options) }
}

/**
 * A specialised [OnboardingActivity] tailored for the (discouraged) cases where a single activity
 * is fulfilling multiple contracts.
 *
 * An actual contract is selected during [onCreate] and cached for the entire lifecycle of the
 * [Activity]. The expectation is that most of the consumers would be able to determine the contract
 * by the [Intent] data.
 */
@DiscouragedOnboardingApi("Migrate to OnboardingActivity")
abstract class CompositeOnboardingActivity<
  I : Any,
  O : Any,
  C : OnboardingActivityApiContract<I, O>,
  > : OnboardingActivity<I, O, C>() {
  /** Selector for a contract [C]. Resolved once per activity lifecycle. */
  protected abstract fun selectContract(intent: Intent): C

  final override val contract: C by lazy { selectContract(rawIntent) }
}
