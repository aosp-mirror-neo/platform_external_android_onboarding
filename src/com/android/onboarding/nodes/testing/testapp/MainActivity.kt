package com.android.onboarding.nodes.testing.testapp

import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.FLAG_ACTIVITY_FORWARD_RESULT
import android.content.pm.PackageManager.ResolveInfoFlags
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.android.onboarding.activity.CompositeOnboardingActivity
import com.android.onboarding.common.TEST_APP
import com.android.onboarding.contracts.ContractResult
import com.android.onboarding.contracts.IdentifyExecutingContractByAction
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.contracts.annotations.DiscouragedOnboardingApi
import com.android.onboarding.contracts.annotations.InternalOnboardingApi
import com.android.onboarding.contracts.annotations.OnboardingNode
import com.android.onboarding.contracts.failNode
import com.android.onboarding.contracts.registerForActivityLaunch

@OptIn(DiscouragedOnboardingApi::class, InternalOnboardingApi::class)
class MainActivity : CompositeOnboardingActivity<ContractArg, String, ColourContract>() {
  private val defaultContract = RedContract()
  private val contracts = arrayOf(defaultContract, BlueContract(), GreenContract())

  override fun selectContract(intent: Intent): ColourContract =
    contracts.firstOrNull { it.isExecuting(intent.action) } ?: defaultContract

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_main)

    if (attachResult.valid) {
      @Suppress("SetTextI18n")
      checkNotNull(findViewById<TextView>(R.id.status)).text = "Received argument: ${argument.arg}"
    }

    val processSpinner = checkNotNull(findViewById<Spinner>(R.id.processSpinner))
    val packages =
      packageManager
        .queryIntentActivities(
          Intent("com.android.onboarding.nodes.testing.testapp.red"),
          ResolveInfoFlags.of(0),
        )
        .map { it.activityInfo.packageName }
        .toSet()
        .toList()
    val packageNames =
      packages
        .map { packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)) }
        .toList()
    processSpinner.adapter =
      ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, packageNames)
    processSpinner.setSelection(packages.indexOf(packageName))

    val contractSpinner = checkNotNull(findViewById<Spinner>(R.id.contractSpinner))
    contractSpinner.adapter =
      ArrayAdapter(
        this,
        android.R.layout.simple_spinner_dropdown_item,
        contracts.map(OnboardingActivityApiContract<*, *>::metadata).map(OnboardingNode::name),
      )
    contractSpinner.setSelection(contracts.indexOf(contract))

    val noResultLaunchers = contracts.map { registerForActivityLaunch(it) }
    val resultLaunchers = contracts.map { registerForActivityResult(it, this::onResult) }

    checkNotNull(findViewById<TextView>(R.id.packageName)).text =
      packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))

    checkNotNull(findViewById<LinearLayout>(R.id.bg)).setBackgroundColor(contract.colour)

    when (contract) {
      is RedContract -> {
        checkNotNull(findViewById<LinearLayout>(R.id.bg)).setBackgroundColor(Color.RED)
      }

      is BlueContract -> {
        checkNotNull(findViewById<LinearLayout>(R.id.bg)).setBackgroundColor(Color.BLUE)
      }

      is GreenContract -> {
        checkNotNull(findViewById<LinearLayout>(R.id.bg)).setBackgroundColor(Color.GREEN)
      }
    }

    checkNotNull(findViewById<Button>(R.id.startActivityAndFinishButton)).setOnClickListener {
      val arg = checkNotNull(findViewById<EditText>(R.id.argument)).text.toString()
      val contractId =
        checkNotNull(findViewById<Spinner>(R.id.contractSpinner)).selectedItemId.toInt()
      val contractLauncher = noResultLaunchers[contractId]
      val targetPackageName = checkNotNull(packages[processSpinner.selectedItemPosition]).toString()

      contractLauncher.launch(ContractArg(arg, targetPackageName))
      finish()
    }

    checkNotNull(findViewById<Button>(R.id.startActivityAndForwardButton)).setOnClickListener {
      val arg = checkNotNull(findViewById<EditText>(R.id.argument)).text.toString()
      val contractId =
        checkNotNull(findViewById<Spinner>(R.id.contractSpinner)).selectedItemId.toInt()
      val contractLauncher = noResultLaunchers[contractId]
      val targetPackageName = checkNotNull(packages[processSpinner.selectedItemPosition]).toString()

      contractLauncher.launch(ContractArg(arg, targetPackageName, shouldForward = true))
      finish()
    }

    checkNotNull(findViewById<Button>(R.id.startActivityButton)).setOnClickListener {
      val arg = checkNotNull(findViewById<EditText>(R.id.argument)).text.toString()
      val contractId =
        checkNotNull(findViewById<Spinner>(R.id.contractSpinner)).selectedItemId.toInt()
      val contractLauncher = noResultLaunchers[contractId]
      val targetPackageName = checkNotNull(packages[processSpinner.selectedItemPosition]).toString()

      contractLauncher.launch(ContractArg(arg, targetPackageName))
    }

    checkNotNull(findViewById<Button>(R.id.startActivityForResultButton)).setOnClickListener {
      val arg = checkNotNull(findViewById<EditText>(R.id.argument)).text.toString()
      val contractId =
        checkNotNull(findViewById<Spinner>(R.id.contractSpinner)).selectedItemId.toInt()
      val contractLauncher = resultLaunchers[contractId]
      val targetPackageName = checkNotNull(packages[processSpinner.selectedItemPosition]).toString()

      contractLauncher.launch(ContractArg(arg, targetPackageName))
    }

    checkNotNull(findViewById<Button>(R.id.finishButton)).setOnClickListener { finish() }
    checkNotNull(findViewById<Button>(R.id.crashButton)).setOnClickListener { error("Crashing, as requested...") }
    checkNotNull(findViewById<Button>(R.id.failNodeButton)).setOnClickListener {
      failNode(checkNotNull(findViewById<EditText>(R.id.argument)).text.toString())
    }
    checkNotNull(findViewById<Button>(R.id.setResultAndFinishButton)).setOnClickListener {
      setResult(checkNotNull(findViewById<EditText>(R.id.argument)).text.toString())
      finish()
    }
  }

  fun onResult(result: String) {
    checkNotNull(findViewById<TextView>(R.id.status)).text = "Received result: $result"
  }
}

data class ContractArg(
  val arg: String,
  val targetPackageName: String,
  val shouldForward: Boolean = false,
)

sealed class ColourContract(val colour: Int, private val name: String) :
  OnboardingActivityApiContract<ContractArg, String>(), IdentifyExecutingContractByAction {
  override fun isExecuting(action: String?) = action?.endsWith(".$name") ?: false

  override fun performCreateIntent(context: Context, arg: ContractArg): Intent =
    Intent("com.android.onboarding.nodes.testing.testapp.$name").apply {
      setPackage(arg.targetPackageName)
      putExtra("KEY", arg.arg)
      addCategory(CATEGORY_DEFAULT)

      if (arg.shouldForward) {
        addFlags(FLAG_ACTIVITY_FORWARD_RESULT)
      }
    }

  override fun performExtractArgument(intent: Intent): ContractArg =
    ContractArg(
      checkNotNull(intent.getStringExtra("KEY")),
      intent.`package` ?: "",
      intent.hasFlag(FLAG_ACTIVITY_FORWARD_RESULT),
    )

  override fun performParseResult(result: ContractResult): String =
    checkNotNull(result.intent?.getStringExtra("KEY"))

  override fun performSetResult(result: String): ContractResult =
    ContractResult.Success(1, Intent().apply { putExtra("KEY", result) })
}

@OnboardingNode(component = TEST_APP, name = "Red", uiType = OnboardingNode.UiType.OTHER)
class RedContract : ColourContract(Color.RED, "red")

@OnboardingNode(component = TEST_APP, name = "Blue", uiType = OnboardingNode.UiType.OTHER)
class BlueContract : ColourContract(Color.BLUE, "blue")

@OnboardingNode(component = TEST_APP, name = "Green", uiType = OnboardingNode.UiType.OTHER)
class GreenContract : ColourContract(Color.GREEN, "green")

fun Intent.hasFlag(flag: Int) = (this.flags and flag) == flag
