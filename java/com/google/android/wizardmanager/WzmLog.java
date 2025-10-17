package com.google.android.wizardmanager;

import android.util.Log;
import androidx.annotation.VisibleForTesting;

/**
 * Use the stack trace to provide contextual annotation (method name, file name, line number) with
 * log messages, while keeping the log tag within its 23 character limit.
 *
 * <p>A format string is applied to the log message, with a default format providing a prefix of the
 * method name and suffix of the file name and line number ("m b [f:n]").
 *
 * <pre>
 *      'b': message body
 *      'm': method name
 *      'f': file name
 *      'n': line number
 *
 *      WizardManager: {method_name} {message_body} [{file_name}:{line_number}]
 * </pre>
 *
 * When logging verbose and debug logs, the logs should either be guarded by if (WzmLog.isV()), or a
 * constant if (DEBUG). That DEBUG constant should be false on any submitted code.
 *
 * <p>[inspired by com.google.android.setupwizard.common.util.Logger]
 */
public final class WzmLog {

  private static final String TAG = "WizardManager";

  // Only use this field for local debugging
  // LINT.DoNotSubmitIf(true)
  private static final boolean DEBUG = false;

  private static final char FORMAT_MESSAGE_BODY = 'b';
  private static final char FORMAT_METHOD_NAME = 'm';
  private static final char FORMAT_FILE_NAME = 'f';
  private static final char FORMAT_LINE_NUMBER = 'n';
  private static final String DEFAULT_FORMAT = "m b [f:n]";

  @VisibleForTesting public static final int STACK_LEVEL = 4;

  @VisibleForTesting
  public static boolean formatRequiresStackTrace(String format) {
    if (DEFAULT_FORMAT.equals(format)) {
      // optimized check for the default format
      return true;
    }
    boolean result = false;
    if (format != null) {
      for (int i = 0, n = format.length(); i < n; i++) {
        char c = format.charAt(i);
        if (c == FORMAT_METHOD_NAME || c == FORMAT_FILE_NAME || c == FORMAT_LINE_NUMBER) {
          result = true;
          break;
        }
      }
    }
    return result;
  }

  // Adjust stackLevel to correct for calling depth. When calling directly from robolectric,
  // use STACK_LEVEL - 2
  static String applyFormat(String message, String format, int stackLevel) {
    if (format == null || format.length() == 0) {
      return message;
    }

    StackTraceElement caller;
    if (formatRequiresStackTrace(format)) {
      StackTraceElement[] stack = Thread.currentThread().getStackTrace();
      if (DEBUG) {
        dumpStack(stack, "applyFormat ", stackLevel);
      }

      caller = stack[stackLevel];
    } else {
      caller = new StackTraceElement("", "", "", 0);
    }

    // a reasonable default length includes all of the available elements
    int maxLen =
        format.length()
            + message.length()
            + caller.getMethodName().length()
            + caller.getFileName().length()
            + 4 /* digits for linenumber */;
    StringBuilder result = new StringBuilder(maxLen);
    boolean messageIncluded = false;
    int length = format.length();
    for (int i = 0; i < length; i++) {
      char c = format.charAt(i);
      switch (c) {
        case FORMAT_MESSAGE_BODY -> {
          result.append(message);
          messageIncluded = true;
        }
        case FORMAT_METHOD_NAME -> {
          // check message for pre-existing method name to avoid dupes
          String methodName = caller.getMethodName();
          if (!methodName.equalsIgnoreCase(message.substring(0, message.indexOf(' ')))) {
            result.append(methodName);
          } else {
            while (i + 1 < length && Character.isWhitespace(format.charAt(i + 1))) {
              // after ignoring the method name from the format string, also ignore
              // any immediately following whitespace
              i++;
            }
          }
        }
        case FORMAT_FILE_NAME -> result.append(caller.getFileName());
        case FORMAT_LINE_NUMBER -> result.append(caller.getLineNumber());
        default -> result.append(c);
      }
    }
    if (!messageIncluded) {
      // if message is not represented in the format, treat the format as a prefix
      result.append(message);
    }
    return result.toString();
  }

  private static void dumpStack(StackTraceElement[] stack, String caller, int stackLevel) {
    Log.d(TAG, "dumpStack stackLevel=" + stackLevel);
    for (int i = 0; i < stack.length; i++) {
      Log.d(TAG, caller + " stack[" + i + "]=" + stack[i].getClassName());
    }
  }

  public static void v(String message) {
    Log.v(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL));
  }

  public static void v(String message, String format) {
    Log.v(TAG, applyFormat(message, format, STACK_LEVEL));
  }

  public static void v(String message, Throwable throwable) {
    Log.v(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL), throwable);
  }

  public static void v(String message, String format, Throwable throwable) {
    Log.v(TAG, applyFormat(message, format, STACK_LEVEL), throwable);
  }

  public static boolean isV() {
    return Log.isLoggable(TAG, Log.VERBOSE);
  }

  public static void d(String message) {
    Log.d(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL));
  }

  public static void d(String message, String format) {
    Log.d(TAG, applyFormat(message, format, STACK_LEVEL));
  }

  public static void d(String message, Throwable throwable) {
    Log.d(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL), throwable);
  }

  public static void d(String message, String format, Throwable throwable) {
    Log.d(TAG, applyFormat(message, format, STACK_LEVEL), throwable);
  }

  public static boolean isD() {
    return Log.isLoggable(TAG, Log.DEBUG);
  }

  public static void i(String message) {
    Log.i(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL));
  }

  public static void i(String message, String format) {
    Log.i(TAG, applyFormat(message, format, STACK_LEVEL));
  }

  public static void i(String message, Throwable throwable) {
    Log.i(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL), throwable);
  }

  public static void i(String message, String format, Throwable throwable) {
    Log.i(TAG, applyFormat(message, format, STACK_LEVEL), throwable);
  }

  // There is no isI() or beyond because those levels are always logged.

  public static void w(String message) {
    Log.w(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL));
  }

  public static void w(String message, String format) {
    Log.w(TAG, applyFormat(message, format, STACK_LEVEL));
  }

  public static void w(String message, Throwable throwable) {
    Log.w(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL), throwable);
  }

  public static void w(String message, String format, Throwable throwable) {
    Log.w(TAG, applyFormat(message, format, STACK_LEVEL), throwable);
  }

  public static void e(String message) {
    Log.e(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL));
  }

  public static void e(String message, String format) {
    Log.e(TAG, applyFormat(message, format, STACK_LEVEL));
  }

  public static void e(String message, Throwable throwable) {
    Log.e(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL), throwable);
  }

  public static void e(String message, String format, Throwable throwable) {
    Log.e(TAG, applyFormat(message, format, STACK_LEVEL), throwable);
  }

  public static void wtf(String message) {
    Log.wtf(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL));
  }

  public static void wtf(String message, String format) {
    Log.wtf(TAG, applyFormat(message, format, STACK_LEVEL));
  }

  public static void wtf(String message, Throwable throwable) {
    Log.wtf(TAG, applyFormat(message, DEFAULT_FORMAT, STACK_LEVEL), throwable);
  }

  public static void wtf(String message, String format, Throwable throwable) {
    Log.wtf(TAG, applyFormat(message, format, STACK_LEVEL), throwable);
  }
}
