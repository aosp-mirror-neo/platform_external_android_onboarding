package com.android.onboarding.nodes.testing.testapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.LinearLayout
import com.android.onboarding.common.TEST_APP
import com.android.onboarding.contracts.ContractResult
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.OnboardingNode

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_main)

    val redLauncher = registerForActivityResult(RedContract()) { onResult(it) }

    val blueLauncher = registerForActivityResult(BlueContract()) { onResult(it) }

    val greenLauncher = registerForActivityResult(GreenContract()) { onResult(it) }

    when (intent?.component?.className) {
      "com.android.onboarding.nodes.testing.testapp.RedActivity" -> {
        findViewById<LinearLayout>(R.id.bg).setBackgroundColor(Color.RED)
        RedContract().validate(this, intent)
      }
      "com.android.onboarding.nodes.testing.testapp.BlueActivity" -> {
        findViewById<LinearLayout>(R.id.bg).setBackgroundColor(Color.BLUE)
        BlueContract().validate(this, intent)
      }
      "com.android.onboarding.nodes.testing.testapp.GreenActivity" -> {
        findViewById<LinearLayout>(R.id.bg).setBackgroundColor(Color.GREEN)
        GreenContract().validate(this, intent)
      }
    }

    findViewById<Button>(R.id.redButton).setOnClickListener { redLauncher.launch(10) }
    findViewById<Button>(R.id.blueButton).setOnClickListener { blueLauncher.launch(10) }
    findViewById<Button>(R.id.greenButton).setOnClickListener { greenLauncher.launch(10) }
  }

  fun onResult(result: Int) {}
}

@OnboardingNode(component = TEST_APP, name = "Red", hasUi = OnboardingNode.HasUi.YES)
class RedContract : OnboardingActivityApiContract<Int, Int>() {
  override fun performCreateIntent(context: Context, arg: Int): Intent =
    Intent("com.android.onboarding.nodes.testing.testapp.red").apply {
      setComponent(
        ComponentName(
          context.packageName,
          "com.android.onboarding.nodes.testing.testapp.RedActivity"
        )
      )
      putExtra("KEY", arg)
    }

  override fun performExtractArgument(intent: Intent): Int = intent.getIntExtra("KEY", -1)

  override fun performParseResult(result: ContractResult): Int = result.resultCode

  override fun performSetResult(result: Int): ContractResult = ContractResult.Success(result)
}

@OnboardingNode(component = TEST_APP, name = "Blue", hasUi = OnboardingNode.HasUi.YES)
class BlueContract : OnboardingActivityApiContract<Int, Int>() {
  override fun performCreateIntent(context: Context, arg: Int): Intent =
    Intent("com.android.onboarding.nodes.testing.testapp.blue").apply {
      setComponent(
        ComponentName(
          context.packageName,
          "com.android.onboarding.nodes.testing.testapp.BlueActivity"
        )
      )
      putExtra("KEY", arg)
    }

  override fun performExtractArgument(intent: Intent): Int = intent.getIntExtra("KEY", -1)

  override fun performParseResult(result: ContractResult): Int = result.resultCode

  override fun performSetResult(result: Int): ContractResult = ContractResult.Success(result)
}

@OnboardingNode(component = TEST_APP, name = "Green", hasUi = OnboardingNode.HasUi.YES)
class GreenContract : OnboardingActivityApiContract<Int, Int>() {
  override fun performCreateIntent(context: Context, arg: Int): Intent =
    Intent("com.android.onboarding.nodes.testing.testapp.green").apply {
      setComponent(
        ComponentName(
          context.packageName,
          "com.android.onboarding.nodes.testing.testapp.GreenActivity"
        )
      )
      putExtra("KEY", arg)
    }

  override fun performExtractArgument(intent: Intent): Int = intent.getIntExtra("KEY", -1)

  override fun performParseResult(result: ContractResult): Int = result.resultCode

  override fun performSetResult(result: Int): ContractResult = ContractResult.Success(result)
}
