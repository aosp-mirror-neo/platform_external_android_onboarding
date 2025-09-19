package com.android.onboarding.utils.persistable

import android.accounts.Account
import android.os.PersistableBundle
import com.android.onboarding.utils.KnownStableEquality
import com.android.onboarding.utils.StableEquality

/**
 * A wrapper around [Account] which can be persisted to disk.
 *
 * @property account which will need OAuth token to be validated for.
 */
data class PersistableAccount(val account: @KnownStableEquality Account) :
  Persistable, StableEquality {
  override fun toPersistableBundle() = toPersistableBundle(account)

  companion object : PersistableDeserializer<PersistableAccount>, PersistableSerializer<Account> {
    private const val KEY_ACCOUNT_NAME = "accountName"
    private const val KEY_ACCOUNT_TYPE = "accountType"

    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      PersistableAccount(
        Account(
          persistableBundle.getString(KEY_ACCOUNT_NAME) ?: error("Account name not found"),
          persistableBundle.getString(KEY_ACCOUNT_TYPE) ?: error("Account type not found"),
        )
      )

    override fun toPersistableBundle(value: Account) = PersistableBundle {
      putString(KEY_ACCOUNT_NAME, value.name)
      putString(KEY_ACCOUNT_TYPE, value.type)
    }
  }
}
