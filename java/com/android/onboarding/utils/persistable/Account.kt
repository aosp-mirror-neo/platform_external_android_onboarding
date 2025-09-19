package com.android.onboarding.utils.persistable

import android.accounts.Account
import android.os.PersistableBundle
import androidx.annotation.VisibleForTesting

/** Converts an account to a [PersistableBundle] for persistence. */
fun toPersistableBundle(account: Account): PersistableBundle {
  return PersistableBundle().apply {
    putString(KEY_ACCOUNT_NAME, account.name)
    putString(KEY_ACCOUNT_TYPE, account.type)
  }
}

/**
 * Extracts [Account] from given [PersistableBundle] which was previously converted using
 * [toPersistableBundle].
 */
fun parseAccount(
  persistableBundle: PersistableBundle,
  onError: ((key: String, error: String) -> Unit)? = null,
): Account? {
  if (!persistableBundle.containsKey(KEY_ACCOUNT_TYPE)) {
    onError?.invoke(KEY_ACCOUNT_TYPE, "unable to find account type")
    return null
  }
  if (!persistableBundle.containsKey(KEY_ACCOUNT_NAME)) {
    onError?.invoke(KEY_ACCOUNT_NAME, "unable to find account type")
    return null
  }
  return Account(
    persistableBundle.getString(KEY_ACCOUNT_NAME) ?: error("Account name not found"),
    persistableBundle.getString(KEY_ACCOUNT_TYPE) ?: error("Account type not found"),
  )
}

@VisibleForTesting const val KEY_ACCOUNT_NAME = "accountName"
@VisibleForTesting const val KEY_ACCOUNT_TYPE = "accountType"
