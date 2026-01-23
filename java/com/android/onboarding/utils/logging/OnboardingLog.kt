package com.android.onboarding.utils.logging

import android.app.Application
import android.util.Log
import com.google.errorprone.annotations.CanIgnoreReturnValue

/**
 * A wrapper over logcat [Log] class that adds the tag "AndroidOnboarding" to all logs and moves the
 * "local" tags to the start of the message.
 */
object OnboardingLog {
  /**
   * The global Logcat tag to use for logging, tags passed in the log methods will be prepended to
   * the messages instead.
   */
  const val TAG = "AndroidOnboarding"

  /** @see Log.VERBOSE */
  const val VERBOSE = Log.VERBOSE
  /** @see Log.DEBUG */
  const val DEBUG = Log.DEBUG
  /** @see Log.INFO */
  const val INFO = Log.INFO
  /** @see Log.WARN */
  const val WARN = Log.WARN
  /** @see Log.ERROR */
  const val ERROR = Log.ERROR
  /** @see Log.ASSERT */
  const val ASSERT = Log.ASSERT

  /** @see Log.isLoggable */
  @JvmStatic fun isLoggable(level: Int) = Log.isLoggable(TAG, level)

  /** @see Log.v */
  @JvmStatic @CanIgnoreReturnValue fun v(tag: String, msg: String) = Log.v(TAG, format(tag, msg))

  /** @see Log.v */
  @JvmStatic
  @CanIgnoreReturnValue
  fun v(tag: String, msg: String, tr: Throwable) = Log.v(TAG, format(tag, msg), tr)

  /** @see Log.d */
  @JvmStatic @CanIgnoreReturnValue fun d(tag: String, msg: String) = Log.d(TAG, format(tag, msg))

  /** @see Log.d */
  @JvmStatic
  @CanIgnoreReturnValue
  fun d(tag: String, msg: String, tr: Throwable) = Log.d(TAG, format(tag, msg), tr)

  /** @see Log.i */
  @JvmStatic @CanIgnoreReturnValue fun i(tag: String, msg: String) = Log.i(TAG, format(tag, msg))

  /** @see Log.i */
  @JvmStatic
  @CanIgnoreReturnValue
  fun i(tag: String, msg: String, tr: Throwable) = Log.i(TAG, format(tag, msg), tr)

  /** @see Log.w */
  @JvmStatic @CanIgnoreReturnValue fun w(tag: String, msg: String) = Log.w(TAG, format(tag, msg))

  /** @see Log.w */
  @JvmStatic
  @CanIgnoreReturnValue
  fun w(tag: String, msg: String, tr: Throwable) = Log.w(TAG, format(tag, msg), tr)

  /** @see Log.w */
  @JvmStatic @CanIgnoreReturnValue fun w(tag: String, tr: Throwable) = Log.w(TAG, tag, tr)

  /** @see Log.e */
  @JvmStatic @CanIgnoreReturnValue fun e(tag: String, msg: String) = Log.e(TAG, format(tag, msg))

  /** @see Log.e */
  @JvmStatic
  @CanIgnoreReturnValue
  fun e(tag: String, msg: String, tr: Throwable) = Log.e(TAG, format(tag, msg), tr)

  /** @see Log.e */
  @JvmStatic @CanIgnoreReturnValue fun e(tag: String, tr: Throwable) = Log.e(TAG, format(tag), tr)

  /** @see Log.wtf */
  @JvmStatic
  @CanIgnoreReturnValue
  fun wtf(tag: String, msg: String) = Log.wtf(TAG, format(tag, msg))

  /** @see Log.wtf */
  @JvmStatic
  @CanIgnoreReturnValue
  fun wtf(tag: String, msg: String, tr: Throwable) = Log.wtf(TAG, format(tag, msg), tr)

  /** @see Log.wtf */
  @JvmStatic
  @CanIgnoreReturnValue
  fun wtf(tag: String, tr: Throwable) = Log.wtf(TAG, format(tag), tr)

  private val packageRef: String? = run {
    if (android.os.Build.VERSION.SDK_INT < 28) return@run null
    val processName = Application.getProcessName() ?: return@run null
    val packageName = processName.split(":").firstOrNull() ?: return@run null
    /**
     * Trims package name by only keeping first letter of the path chunks and leaving last chunk
     * untouched.
     * - Input: `com.google.app`
     * - Output: `c.g.app`
     */
    val chunks = packageName.split(".")
    val (parts, name) =
      if (chunks.size > 1) {
        chunks.dropLast(1) to chunks.last()
      } else {
        return@run packageName
      }
    return@run parts.joinToString(separator = ".", postfix = ".$name") { it.first().toString() }
  }

  private fun format(tag: String, msg: String? = null) = buildString {
    if (packageRef == null) append(tag) else append("$tag[$packageRef]")
    if (msg != null) {
      append(": $msg")
    }
  }

  /** Create a pre-tagged [OnboardingLog.Logger] for [tag]. */
  @JvmStatic fun logger(tag: String) = Logger(tag)

  /**
   * Pre-tagged [OnboardingLog.Logger] which logs to [OnboardingLog].
   *
   * @property tag the log tag.
   */
  @Suppress("RemoveRedundantQualifierName")
  class Logger(private val tag: String) {
    /** @see Log.v */
    @CanIgnoreReturnValue fun v(msg: String) = OnboardingLog.v(tag, msg)

    /** @see Log.v */
    @CanIgnoreReturnValue fun v(msg: String, tr: Throwable) = OnboardingLog.v(tag, msg, tr)

    /** @see Log.d */
    @CanIgnoreReturnValue fun d(msg: String) = OnboardingLog.d(tag, msg)

    /** @see Log.d */
    @CanIgnoreReturnValue fun d(msg: String, tr: Throwable) = OnboardingLog.d(tag, msg, tr)

    /** @see Log.i */
    @CanIgnoreReturnValue fun i(msg: String) = OnboardingLog.i(tag, msg)

    /** @see Log.i */
    @CanIgnoreReturnValue fun i(msg: String, tr: Throwable) = OnboardingLog.i(tag, msg, tr)

    /** @see Log.w */
    @CanIgnoreReturnValue fun w(msg: String) = OnboardingLog.w(tag, msg)

    /** @see Log.w */
    @CanIgnoreReturnValue fun w(msg: String, tr: Throwable) = OnboardingLog.w(tag, msg, tr)

    /** @see Log.w */
    @CanIgnoreReturnValue fun w(tr: Throwable) = OnboardingLog.w(tag, tr)

    /** @see Log.e */
    @CanIgnoreReturnValue fun e(msg: String) = OnboardingLog.e(tag, msg)

    /** @see Log.e */
    @CanIgnoreReturnValue fun e(msg: String, tr: Throwable) = OnboardingLog.e(tag, msg, tr)

    /** @see Log.e */
    @CanIgnoreReturnValue fun e(tr: Throwable) = OnboardingLog.e(tag, tr)

    /** @see Log.wtf */
    @CanIgnoreReturnValue fun wtf(msg: String) = OnboardingLog.wtf(tag, msg)

    /** @see Log.wtf */
    @CanIgnoreReturnValue fun wtf(msg: String, tr: Throwable) = OnboardingLog.wtf(tag, msg, tr)

    /** @see Log.wtf */
    @CanIgnoreReturnValue fun wtf(tr: Throwable) = OnboardingLog.wtf(tag, tr)
  }
}
