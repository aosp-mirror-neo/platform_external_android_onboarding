package com.google.android.wizardmanager

/**
 * Using an XML-based script, {@link WizardManager} executes a series of actions, with branching
 * based on the returned result codes.
 */
object WizardManagerConstants {

  const val ACTION_LOAD = "com.android.wizard.LOAD"
  const val ACTION_NEXT = "com.android.wizard.NEXT"

  /**
   * Boolean indicating whether wizard manager is running under an activity test. Wizard manager
   * should not do any processing and finish immediately in that case. An item in
   * {@link #EXTRA_WIZARD_BUNDLE}.
   */
  const val KEY_IS_ACTIVITY_TEST = "isActivityTest"

  /**
   * String extra containing the uri of a script to be run. Example:
   * "android.resource://com.google.android.setupwizard/xml/wizard_script_carrier"
   */
  const val EXTRA_LOAD_SCRIPT_URI = "loadScriptUri"

  /** String extra containing the node of a wizard action to be run. Example: "contract:" */
  const val EXTRA_SCRIPT_CONTRACT_NODE = "ScriptContractNode"

  /**
   * Parcelable instance of the WizardAction associated with an intent. An item in
   * {@link #EXTRA_WIZARD_BUNDLE}.
   *
   * @deprecated Use {@link WizardBundle#KEY_ACTION}
   */
  const val KEY_ACTION = "action"

  /** Int extra containing result code returned by a scripted intent */
  const val EXTRA_RESULT_CODE = "com.android.setupwizard.ResultCode"

  /**
   * Parcelable extra containing the internal reference for an intent dispatched by Wizard Manager
   */
  const val EXTRA_WIZARD_BUNDLE = "wizardBundle"

  /**
   * Parcelable instance of the WizardStack associated with an intent. An item in
   * {@link #EXTRA_WIZARD_BUNDLE}.
   *
   * @deprecated Use {@link WizardBundle#KEY_STACK}
   */
  const val KEY_STACK = "stack"
}
