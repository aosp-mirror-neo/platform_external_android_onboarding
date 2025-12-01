package com.android.onboarding.contracts.provisioning

import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.NullSafePersistableBundleExtensions.getStringOrThrow
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer

/**
 * The content of terms and conditions that Enterprise/EMMs have set and would like to be shown to
 * the user as they consent to provisioning.
 *
 * @property header The header of the disclaimer. This is typically the company name of mobile
 *   device management application (MDM) or the organization name.
 * @property content The content of the disclaimer. Styled text is supported. This is parsed by
 *   [android.text.Html.fromHtml] and displayed in a [android.widget.TextView].
 */
data class ProvisioningDisclaimer(val header: String, val content: String) :
  Persistable, StableEquality {
  override fun toPersistableBundle() =
    PersistableBundle().apply {
      putString(KEY_DISCLAIMER_HEADER, header)
      putString(KEY_DISCLAIMER_CONTENT, content)
    }

  companion object : PersistableDeserializer<ProvisioningDisclaimer> {
    private const val KEY_DISCLAIMER_HEADER = "disclaimerHeader"
    private const val KEY_DISCLAIMER_CONTENT = "disclaimerContent"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      ProvisioningDisclaimer(
        persistableBundle.getStringOrThrow(KEY_DISCLAIMER_HEADER),
        persistableBundle.getStringOrThrow(KEY_DISCLAIMER_CONTENT),
      )
  }
}
