@file:Suppress("DefaultPackage")

/**
 * A stub for all unsupported shim methods. Getting this value will always throw an
 * [UnsupportedOperationException].
 */
val unsupported: Nothing
  get() = throw UnsupportedOperationException("Shim")
