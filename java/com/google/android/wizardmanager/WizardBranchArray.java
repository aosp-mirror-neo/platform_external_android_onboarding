package com.google.android.wizardmanager;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.SparseArray;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.onboarding.utils.StableEquality;
import com.android.onboarding.utils.persistable.Persistable;
import java.util.List;

/**
 * A data structure for the branches from a WizardAction, mapping result codes to next actions, with
 * the addition of a default action.
 */
@Keep
public class WizardBranchArray extends SparseArray<String>
    implements Parcelable, Persistable, StableEquality {

  // These can be made private if converted to Kotlin and the Deserializer moved
  static final String DEFAULT_ACTION_ID_KEY = "default_action_id";
  static final String MAP_KEY = "map";
  static final String SPLITTER = ",";

  /** A comma-separated string of action IDs to be used when no specific result code is matched. */
  private String defaultActionIds;

  /**
   * Sets the default action id.
   *
   * @param action The default action id.
   */
  public void setDefaultActionId(String action) {
    defaultActionIds = action;
  }

  /**
   * Sets the default action ids.
   *
   * @param actionIds The default action ids.
   */
  public void setDefaultActionIds(List<String> actionIds) {
    defaultActionIds = TextUtils.join(SPLITTER, actionIds);
  }

  /**
   * Returns the default action id.
   *
   * @return The default action id.
   */
  @Nullable
  public String getDefaultActionId() {
    List<String> actionIds = getDefaultActionIds();
    if (actionIds == null || actionIds.isEmpty()) {
      return null;
    } else {
      return actionIds.get(0);
    }
  }

  /**
   * Returns the default action ids.
   *
   * @return The default action ids.
   */
  @Nullable
  public List<String> getDefaultActionIds() {
    if (defaultActionIds == null) {
      return null;
    } else {
      return List.of(defaultActionIds.split(SPLITTER));
    }
  }

  /**
   * Returns the action id for the given result code.
   *
   * @param resultCode The result code.
   * @return The action id.
   */
  @Nullable
  public String getActionId(int resultCode) {
    List<String> actionIds = getActionIds(resultCode);
    if (actionIds == null || actionIds.isEmpty()) {
      return null;
    } else {
      return actionIds.get(0);
    }
  }

  /**
   * Returns the action ids for the given result code.
   *
   * @param resultCode The result code.
   * @return The action ids.
   */
  @Nullable
  public List<String> getActionIds(int resultCode) {
    String actionIds = get(resultCode, defaultActionIds);
    if (actionIds == null) {
      return null;
    } else {
      return List.of(actionIds.split(SPLITTER));
    }
  }

  /**
   * Sets the action ids for the given result code.
   *
   * @param resultCode The result code.
   * @param actionIds The action ids.
   */
  public void put(int resultCode, List<String> actionIds) {
    put(resultCode, TextUtils.join(SPLITTER, actionIds));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WizardBranchArray) {
      WizardBranchArray o = (WizardBranchArray) obj;
      boolean equalDefault =
          defaultActionIds != null
              ? defaultActionIds.equals(o.defaultActionIds)
              : o.defaultActionIds == null;
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
    int hashCode = defaultActionIds != null ? defaultActionIds.hashCode() : 0;
    // SparseArray doesn't have hashCode implemented so we have to roll our own
    for (int i = 0; i < size(); i++) {
      String val = get(keyAt(i));
      hashCode = 31 * hashCode + (val != null ? val.hashCode() : 0);
    }
    return hashCode;
  }

  @Override
  public String toString() {
    return super.toString() + " default=" + defaultActionIds;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(defaultActionIds);
    dest.writeSparseArray((SparseArray) this);
  }

  public static final Parcelable.Creator<WizardBranchArray> CREATOR =
      new Parcelable.Creator<WizardBranchArray>() {
        @Override
        @SuppressWarnings("unchecked")
        public WizardBranchArray createFromParcel(Parcel source) {
          WizardBranchArray transitions = new WizardBranchArray();
          transitions.defaultActionIds = source.readString();
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
    bundle.putString(DEFAULT_ACTION_ID_KEY, defaultActionIds);

    PersistableBundle map = new PersistableBundle();
    for (int i = 0; i < size(); i++) {
      map.putString(String.valueOf(keyAt(i)), valueAt(i));
    }
    bundle.putPersistableBundle(MAP_KEY, map);

    return bundle;
  }
}
