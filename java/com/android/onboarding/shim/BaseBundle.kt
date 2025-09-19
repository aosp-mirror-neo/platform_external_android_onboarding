package android.os

import unsupported

abstract class BaseBundle {
  fun keySet(): Set<String?> = unsupported

  fun get(k: String?): Any? = unsupported

  fun putInt(k: String?, v: Int): Unit = unsupported

  fun getInt(k: String?, default: Int? = null): Int = unsupported

  fun putLong(k: String?, v: Long): Unit = unsupported

  fun getLong(k: String?, default: Long? = null): Long = unsupported

  fun putString(k: String?, v: String?): Unit = unsupported

  fun getString(k: String?): String? = unsupported

  fun putStringArray(key: String?, value: Array<out String?>?): Unit = unsupported

  fun getStringArray(key: String?): Array<String>? = unsupported
}
