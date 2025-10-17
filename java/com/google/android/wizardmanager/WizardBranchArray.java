package com.google.android.wizardmanager;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.SparseArray;
import androidx.annotation.NonNull;
import com.android.onboarding.utils.StableEquality;
import com.android.onboarding.utils.persistable.Persistable;

/**
 * A data structure for the branches from a WizardAction, mapping result codes to next actions, with
 * the addition of a default action.
 */
public class WizardBranchArray extends SparseArray<String>
    implements Parcelable, Persistable, StableEquality {

  // These can be made private if converted to Kotlin and the Deserializer moved
  static final String DEFAULT_ACTION_ID_KEY = "default_action_id";
  static final String MAP_KEY = "map";

  private String defaultActionId;

  public void setDefaultActionId(String action) {
    defaultActionId = action;
  }

  public String getDefaultActionId() {
    return defaultActionId;
  }

  public String getActionId(int resultCode) {
    return get(resultCode, defaultActionId);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WizardBranchArray) {
      WizardBranchArray o = (WizardBranchArray) obj;
      boolean equalDefault =
          defaultActionId != null
              ? defaultActionId.equals(o.defaultActionId)
              : o.defaultActionId == null;
      if (!equalDefault) {
        return false;
      }

      // For some reason SparseArray does not implement equals
      if (size() != o.size()) {
        return false;
      }
      for (int i = 0; i < size(); i++) {
        int key = keyAt(i);
        String val1 = get(key);
        String val2 = o.get(key);
        boolean equals = val1 != null ? val1.equals(val2) : val2 == null;
        if (!equals) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hashCode = defaultActionId != null ? defaultActionId.hashCode() : 0;
    // SparseArray doesn't have hashCode implemented so we have to roll our own
    for (int i = 0; i < size(); i++) {
      String val = get(keyAt(i));
      hashCode = 31 * hashCode + (val != null ? val.hashCode() : 0);
    }
    return hashCode;
  }

  @Override
  public String toString() {
    return super.toString() + " default=" + defaultActionId;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(defaultActionId);
    dest.writeSparseArray((SparseArray) this);
  }

  public static final Parcelable.Creator<WizardBranchArray> CREATOR =
      new Parcelable.Creator<WizardBranchArray>() {
        @Override
        @SuppressWarnings("unchecked")
        public WizardBranchArray createFromParcel(Parcel source) {
          WizardBranchArray transitions = new WizardBranchArray();
          transitions.defaultActionId = source.readString();
          SparseArray<String> actions = source.readSparseArray(null);
          for (int i = 0; i < actions.size(); i++) {
            transitions.put(actions.keyAt(i), actions.valueAt(i));
          }
          return transitions;
        }

        @Override
        public WizardBranchArray[] newArray(int size) {
          return new WizardBranchArray[size];
        }
      };

  @NonNull
  @Override
  public PersistableBundle toPersistableBundle() {
    PersistableBundle bundle = new PersistableBundle();
    bundle.putString(DEFAULT_ACTION_ID_KEY, defaultActionId);

    PersistableBundle map = new PersistableBundle();
    for (int i = 0; i < size(); i++) {
      map.putString(String.valueOf(keyAt(i)), valueAt(i));
    }
    bundle.putPersistableBundle(MAP_KEY, map);

    return bundle;
  }
}
