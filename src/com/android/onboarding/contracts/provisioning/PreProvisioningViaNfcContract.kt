package com.android.onboarding.contracts.provisioning

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Parcelable
import com.android.onboarding.contracts.IntentScope
import com.android.onboarding.contracts.ScopedIntentSerializer
import com.android.onboarding.contracts.VoidOnboardingActivityApiContract
import javax.inject.Inject

data class PreProvisioningViaNfcArguments(
  val ndefMessages: Array<Parcelable>,
)

class PreProvisioningViaNfcContract @Inject constructor() :
  VoidOnboardingActivityApiContract<PreProvisioningViaNfcArguments>(),
  ScopedIntentSerializer<PreProvisioningViaNfcArguments> {

  override fun performCreateIntent(context: Context, arg: PreProvisioningViaNfcArguments): Intent =
    Intent(NfcAdapter.ACTION_NDEF_DISCOVERED).also { write(it, arg) }

  override fun performExtractArgument(intent: Intent): PreProvisioningViaNfcArguments = read(intent)

  override fun IntentScope.write(value: PreProvisioningViaNfcArguments) {
    this[NfcAdapter.EXTRA_NDEF_MESSAGES] = value.ndefMessages
  }

  override fun IntentScope.read(): PreProvisioningViaNfcArguments =
    PreProvisioningViaNfcArguments(
      ndefMessages = parcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
    )
}
