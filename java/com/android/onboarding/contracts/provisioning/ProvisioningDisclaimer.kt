package com.android.onboarding.contracts.provisioning

import android.net.Uri
import android.os.PersistableBundle
import androidx.core.net.toUri
import com.android.onboarding.utils.KnownStableEquality
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.NullSafePersistableBundleExtensions.getStringOrThrow
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer

/**
 * Data class representing the content of terms and conditions that Enterprise/EMMs have set and
 * would like to be shown in the consent screen when user is consenting to the provisioning.
 *
 * @property header The header of the disclaimer.
 * @property content The content of the disclaimer as a [Uri].
 */
data class ProvisioningDisclaimer(val header: String, val content: @KnownStableEquality Uri) :
  Persistable, StableEquality {
  override fun toPersistableBundle() =
    PersistableBundle().apply {
      putString(KEY_DISCLAIMER_HEADER, header)
      putString(KEY_DISCLAIMER_CONTENT, content.toString())
    }

  companion object : PersistableDeserializer<ProvisioningDisclaimer> {
    private const val KEY_DISCLAIMER_HEADER = "disclaimerHeader"
    private const val KEY_DISCLAIMER_CONTENT = "disclaimerContent"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      ProvisioningDisclaimer(
        persistableBundle.getStringOrThrow(KEY_DISCLAIMER_HEADER),
        persistableBundle.getStringOrThrow(KEY_DISCLAIMER_CONTENT).toUri(),
      )
  }
}
