package com.google.android.wizardmanager;

import static com.android.onboarding.utils.persistable.PersistableBundleKt.putPersistable;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import com.android.onboarding.utils.StableEquality;
import com.android.onboarding.utils.persistable.Persistable;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to maintain a stack of WizardAction objects. When a script is loaded, it will be
 * pushed onto this stack. When exited, it will be popped. A parcelled copy of the current stack is
 * included in the bundled extra attached to each action executed by setup wizard, and is utilized
 * when returning from one script to its caller.
 */
@Keep
public class WizardStack implements Parcelable, Persistable, StableEquality {

  // These can be made private if converted to Kotlin and the Deserializer moved
  static final String SIZE_KEY = "size";

  /* static section */

  public static void dump(WizardStack instance) {
    int i = 0;
    if (instance != null) {
      for (WizardAction action : instance.stack) {
        WzmLog.d("DUMP action[" + i++ + "]=");
        WizardAction.dump("  ", action);
      }
    } else {
      WzmLog.d("DUMP WizardStack instance is null");
    }
  }

  /* non-static section */

  @NonNull private final List<WizardAction> stack = new ArrayList<>();

  public WizardStack() {}

  public WizardStack(Parcel parcel) {
    parcel.readTypedList(stack, WizardAction.CREATOR);
  }

  public WizardStack(WizardStack other) {
    this.stack.addAll(other.stack);
  }

  public WizardAction pop() {
    return stack.remove(stack.size() - 1);
  }

  public void push(WizardAction wizardAction) {
    stack.add(wizardAction);
  }

  public boolean isEmpty() {
    return stack.isEmpty();
  }

  public WizardAction peek() {
    if (stack.isEmpty()) {
      throw new IllegalStateException("Cannot get current action on empty stack");
    }
    return stack.get(stack.size() - 1);
  }

  public void setTop(WizardAction wizardAction) {
    if (stack.isEmpty()) {
      throw new IllegalStateException("Cannot replace current action on empty stack");
    }
    stack.set(stack.size() - 1, wizardAction);
  }

  @Override
  public int hashCode() {
    return stack.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof WizardStack)) {
      return false;
    }
    return stack.equals(((WizardStack) o).stack);
  }

  @Override
  public String toString() {
    return stack.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeTypedList(stack);
  }

  public static final Creator<WizardStack> CREATOR =
      new Creator<WizardStack>() {

        @Override
        public WizardStack createFromParcel(Parcel source) {
          return new WizardStack(source);
        }

        @Override
        public WizardStack[] newArray(int size) {
          return new WizardStack[size];
        }
      };

  public String indentedToString(boolean verbose) {
    StringBuilder bldr = new StringBuilder();
    for (int i = stack.size() - 1; i >= 0; --i) {
      WizardAction action = stack.get(i);
      String actAsString = null;
      if (verbose) {
        actAsString = action.indentedToString();
      } else {
        actAsString = action.getId();
        // shorter script names
        if (actAsString != null) {
          actAsString =
              actAsString
                  .replace("android.resource://", "")
                  .replaceFirst("com.google.android.setupwizard/xml", "suw");
        }
      }
      bldr.append(actAsString).append(",");
    }
    if (bldr.length() > 0) {
      bldr.setLength(bldr.length() - 1);
    }
    return bldr.toString();
  }

  @NonNull
  @Override
  public PersistableBundle toPersistableBundle() {
    PersistableBundle bundle = new PersistableBundle();

    bundle.putInt(SIZE_KEY, stack.size());
    for (int i = 0; i < stack.size(); i++) {
      putPersistable(bundle, String.valueOf(i), stack.get(i));
    }

    return bundle;
  }
}
