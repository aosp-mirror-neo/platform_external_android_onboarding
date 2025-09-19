package com.android.onboarding.metadata;

import com.google.errorprone.annotations.CompileTimeConstant;
import java.util.Objects;

/** Wrapper around a non-privacy-sensitive string. */
// This is implemented in Java due to b/187253217
public final class NonSensitiveString implements IString {

  @CompileTimeConstant public final String value;

  @Override
  public String getValue() {
    return value;
  }

  public NonSensitiveString(@CompileTimeConstant String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NonSensitiveString) {
      return Objects.equals(((NonSensitiveString) obj).value, value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public NonSensitiveString toNonSensitiveString() {
    return this;
  }

  @Override
  public String toString() {
    return "NonSensitiveString(" + getValue() + ")";
  }
}
