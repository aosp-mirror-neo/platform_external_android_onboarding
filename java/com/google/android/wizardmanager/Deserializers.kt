package com.google.android.wizardmanager

import android.os.PersistableBundle
import com.android.onboarding.config.OnboardingConfig
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.getPersistableList
import com.google.android.wizardmanager.WizardAction.BRANCHES_KEY
import com.google.android.wizardmanager.WizardAction.CONFIG_KEY
import com.google.android.wizardmanager.WizardAction.DEPENDENCIES_KEY
import com.google.android.wizardmanager.WizardAction.FLOW_KEY
import com.google.android.wizardmanager.WizardAction.ID_KEY
import com.google.android.wizardmanager.WizardAction.IF_FLOW_KEY
import com.google.android.wizardmanager.WizardAction.IF_LIFECYCLE_KEY
import com.google.android.wizardmanager.WizardAction.INDEX_KEY
import com.google.android.wizardmanager.WizardAction.NESTED_SCRIPT_KEY
import com.google.android.wizardmanager.WizardAction.NODE_KEY
import com.google.android.wizardmanager.WizardAction.SCRIPT_URI_KEY
import com.google.android.wizardmanager.WizardAction.URI_KEY
import com.google.android.wizardmanager.WizardBranchArray.DEFAULT_ACTION_ID_KEY
import com.google.android.wizardmanager.WizardBranchArray.MAP_KEY
import com.google.android.wizardmanager.WizardScript.ACTION_LIST_KEY
import com.google.android.wizardmanager.WizardScript.CONFIG_KEY as WIZARD_SCRIPT_CONFIG_KEY
import com.google.android.wizardmanager.WizardScript.FIRST_ACTION_INDEX_KEY
import com.google.android.wizardmanager.WizardScript.SCRIPT_URI_KEY as WIZARD_SCRIPT_SCRIPT_URI_KEY
import com.google.android.wizardmanager.WizardScript.VERSION_KEY
import com.google.android.wizardmanager.WizardStack.SIZE_KEY

// Ideally the objects in this file should be companion objects to the classes themselves, but
// we can't do that until they are converted to Kotlin

object Deserializers {
  object WizardAction : PersistableDeserializer<com.google.android.wizardmanager.WizardAction> {
    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): com.google.android.wizardmanager.WizardAction {
      val scriptUri = persistableBundle.getString(SCRIPT_URI_KEY) ?: error("Missing SCRIPT_URI")
      val index = persistableBundle.getInt(INDEX_KEY)
      val branches =
        persistableBundle.getPersistable(BRANCHES_KEY, WizardBranchArray)
          ?: error("Missing BRANCHES")
      return com.google.android.wizardmanager.WizardAction.Builder(scriptUri, index)
        .id(persistableBundle.getString(ID_KEY))
        .uri(persistableBundle.getString(URI_KEY))
        .node(persistableBundle.getString(NODE_KEY))
        .ifLifecycle(persistableBundle.getString(IF_LIFECYCLE_KEY))
        .flow(persistableBundle.getString(FLOW_KEY))
        .ifFlow(persistableBundle.getString(IF_FLOW_KEY))
        .branches(branches)
        .dependencies(persistableBundle.getStringArray(DEPENDENCIES_KEY)?.toList() ?: emptyList())
        .config(persistableBundle.getPersistable(CONFIG_KEY, OnboardingConfig))
        .nestedScript(persistableBundle.getPersistable(NESTED_SCRIPT_KEY, WizardScript))
        .build()
    }
  }

  object WizardScript : PersistableDeserializer<com.google.android.wizardmanager.WizardScript> {
    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): com.google.android.wizardmanager.WizardScript {
      val scriptUri =
        persistableBundle.getString(WIZARD_SCRIPT_SCRIPT_URI_KEY) ?: error("Missing SCRIPT_URI")
      val actionList =
        persistableBundle.getPersistableList(ACTION_LIST_KEY, WizardAction)
          ?: error("Missing ACTION_LIST")
      val firstActionIndex = persistableBundle.getInt(FIRST_ACTION_INDEX_KEY)
      val version = persistableBundle.getInt(VERSION_KEY)
      val config = persistableBundle.getPersistable(WIZARD_SCRIPT_CONFIG_KEY, OnboardingConfig)
      return WizardScript(scriptUri, actionList, firstActionIndex, version, config)
    }
  }

  object WizardBranchArray :
    PersistableDeserializer<com.google.android.wizardmanager.WizardBranchArray> {
    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): com.google.android.wizardmanager.WizardBranchArray =
      WizardBranchArray().also {
        it.defaultActionId = persistableBundle.getString(DEFAULT_ACTION_ID_KEY)
        val map = persistableBundle.getPersistableBundle(MAP_KEY) ?: error("Missing map")
        for (k in map.keySet()) {
          it.put(Integer.valueOf(k), map.getString(k))
        }
      }
  }

  object WizardStack : PersistableDeserializer<com.google.android.wizardmanager.WizardStack> {
    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): com.google.android.wizardmanager.WizardStack =
      WizardStack().also {
        val size = persistableBundle.getInt(SIZE_KEY)

        for (i in 0..<size) {
          it.push(persistableBundle.getPersistable(i.toString(), WizardAction))
        }
      }
  }
}
