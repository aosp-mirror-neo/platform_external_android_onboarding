package android.os

import unsupported

/** Shim for android.os.Bundle */
class Bundle(persistableBundle: PersistableBundle) {
  fun putBundle(k: String, v: Bundle?): Unit = unsupported

  fun getBundle(k: String): Bundle? = unsupported
}
