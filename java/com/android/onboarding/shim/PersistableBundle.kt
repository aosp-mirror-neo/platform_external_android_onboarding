package android.os

import unsupported

/** Shim for android.os.PersistableBundle */
class PersistableBundle : BaseBundle() {
  fun getPersistableBundle(k: String?): PersistableBundle? = unsupported

  fun putPersistableBundle(k: String?, v: PersistableBundle?): Unit = unsupported
}
