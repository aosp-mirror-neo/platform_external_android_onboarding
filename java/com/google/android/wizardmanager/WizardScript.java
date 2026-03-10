package com.google.android.wizardmanager;

import static com.android.onboarding.utils.persistable.PersistableBundleKt.putPersistable;
import static com.android.onboarding.utils.persistable.PersistableBundleKt.putPersistableList;
import static com.google.android.wizardmanager.WizardAction.YIELD_ACTION;
import static com.google.android.wizardmanager.WizardAction.YIELD_ACTION_ID;
import static com.google.common.collect.ImmutableList.toImmutableList;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.android.onboarding.config.OnboardingConfig;
import com.android.onboarding.utils.StableEquality;
import com.android.onboarding.utils.persistable.Persistable;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Defines a collection of WizardActions to be run in sequential order. Instances should be
 * immutable.
 */
public class WizardScript
    implements Parcelable, Iterable<WizardAction>, Persistable, StableEquality {

  // These can be made private if converted to Kotlin and the Deserializer moved
  static final String SCRIPT_URI_KEY = "script_uri";
  static final String ACTION_LIST_KEY = "action_list";
  static final String FIRST_ACTION_INDEX_KEY = "first_action_index";
  static final String VERSION_KEY = "version";
  static final String CONFIG_KEY = "config";

  // Only use this field for local debugging
  // LINT.DoNotSubmitIf(true)
  private static final boolean DEBUG = false;

  private final String scriptUri;
  private final int firstActionIndex;
  private final List<WizardAction> actionList;
  private final Map<String, WizardAction> actions;
  private final int version;
  @Nullable private final OnboardingConfig config;

  private String customizedFlow = null;
  private Integer exitResultCode = null;

  public WizardScript(String scriptUri, List<WizardAction> actionList, int version) {
    this(scriptUri, actionList, 0, version, /* config= */ null);
  }

  protected WizardScript(WizardScript script) {
    this(
        script.scriptUri,
        script.actionList,
        script.firstActionIndex,
        script.version,
        script.config);
    customizedFlow = script.customizedFlow;
    exitResultCode = script.exitResultCode;
  }

  public WizardScript(
      String scriptUri,
      List<WizardAction> actionList,
      int firstActionIndex,
      int version,
      @Nullable OnboardingConfig config) {
    this.scriptUri = scriptUri;
    this.firstActionIndex = firstActionIndex;
    this.actionList = Collections.unmodifiableList(actionList);
    this.version = version;
    this.config = config;

    Map<String, WizardAction> actionMap = new LinkedHashMap<>();
    for (WizardAction action : actionList) {
      actionMap.put(action.getId(), action);
    }
    actions = Collections.unmodifiableMap(actionMap);
  }

  public String getScriptUri() {
    return scriptUri;
  }

  public int getVersion() {
    return version;
  }

  @VisibleForTesting
  public Map<String, WizardAction> getAllActions() {
    return actions;
  }

  /** Returns the unmodifiable list of all actions in this script, in sequential order. */
  public List<WizardAction> getActions() {
    return actionList;
  }

  public WizardAction getAction(int index) {
    if (DEBUG) {
      WzmLog.d("getAction script=" + scriptUri + " index=" + index + " mActionList=" + actionList);
    }
    return actionList.get(index);
  }

  public WizardAction getAction(String actionId) {
    if (DEBUG) {
      WzmLog.d(
          "getAction script="
              + scriptUri
              + " actionId="
              + actionId
              + " mActions="
              + actions
              + " mFirstActionIndex="
              + firstActionIndex);
    }
    return actions.get(actionId);
  }

  public WizardAction getFirstAction() {
    return getAction(firstActionIndex);
  }

  public void setCustomizedFlow(String flow) {
    customizedFlow = flow;
  }

  @Nullable
  public String getCustomizedFlow() {
    return customizedFlow;
  }

  public void saveExitResultCode(Integer resultCode) {
    exitResultCode = resultCode;
  }

  @Nullable
  public Integer getExitResultCode() {
    return exitResultCode;
  }

  /**
   * Return the next scripted action, given the index of an action and its returned result code
   *
   * @param index of an action in this script
   * @param resultCode for the indexed action
   * @return next scripted action; at the end of the script, return null
   */
  @Nullable
  public WizardAction getNextAction(int index, int resultCode) {
    final WizardAction currentAction = getCurrentAction(index, resultCode);
    if (currentAction == null) {
      WzmLog.v("getNextAction currentAction is null");
      return null;
    }

    String nextActionId = currentAction.getNextActionId(resultCode);

    WizardAction nextAction = getNextActionByIdAndIndex(nextActionId, index);

    if (DEBUG || WzmLog.isV()) {
      WzmLog.v(
          "getNextAction script="
              + scriptUri
              + " index="
              + index
              + " resultCode="
              + resultCode
              + " uri="
              + currentAction.getUri()
              + " nextActionId="
              + nextActionId
              + " nextAction="
              + nextAction);
    }

    return nextAction;
  }

  @Nullable
  public List<WizardAction> getNextActions(int index, int resultCode) {
    final WizardAction currentAction = getCurrentAction(index, resultCode);
    if (currentAction == null) {
      return null;
    }

    List<String> nextActionIds = currentAction.getNextActionIds(resultCode);
    if (nextActionIds == null) {
      // When there is no next action defined, look up the next action by index.
      WizardAction nextAction = getNextActionByIdAndIndex(null, index);
      if (nextAction != null) {
        return ImmutableList.of(nextAction);
      } else {
        // No next action found, return null as end of script.
        return null;
      }
    }

    ImmutableList<WizardAction> nextActions =
        nextActionIds.stream()
            .map(
                actionId -> {
                  WizardAction nextAction = getNextActionByIdAndIndex(actionId, index);
                  if (DEBUG || WzmLog.isV()) {
                    WzmLog.v(
                        "getNextAction script="
                            + scriptUri
                            + " index="
                            + index
                            + " resultCode="
                            + resultCode
                            + " uri="
                            + currentAction.getUri()
                            + " nextActionId="
                            + actionId
                            + " nextAction="
                            + nextAction);
                  }
                  return nextAction;
                })
            .filter(Objects::nonNull)
            .collect(toImmutableList());
    return nextActions;
  }

  @Nullable
  private WizardAction getCurrentAction(int index, int resultCode) {
    if (resultCode == Activity.RESULT_CANCELED) {
      if (DEBUG || WzmLog.isV()) {
        WzmLog.v(
            "getNextAction script="
                + scriptUri
                + " index="
                + index
                + " resultCode="
                + resultCode
                + " RESULT_CANCELED not expected; ignored");
      }
      return null;
    }

    WizardAction currentAction = actionList.get(index);

    if (currentAction == null) {
      // this should never happen during normal usage, aside from unit testing
      if (DEBUG || WzmLog.isV()) {
        WzmLog.v(
            "getNextAction script="
                + scriptUri
                + " index="
                + index
                + " resultCode="
                + resultCode
                + " no action at this index; ignored");
      }
    }

    return currentAction;
  }

  /**
   * Get the next action by id and current index. If the next action is not found, increment the
   * current index to get the next action.
   *
   * <p>if the next action is null, it means the end of the script.
   *
   * <p>if the next action is <@link WizardAction#YIELD_ACTION}, it means the script is yielded.
   *
   * @param nextActionId the next action id
   * @param currentIndex the current index
   * @return the next action or null if the next action is not found
   */
  @Nullable
  private WizardAction getNextActionByIdAndIndex(String nextActionId, int currentIndex) {
    WizardAction action = null;
    try {
      if (nextActionId != null) {
        if (TextUtils.equals(nextActionId, YIELD_ACTION_ID)) {
          return YIELD_ACTION;
        } else {
          // if the next action is referenced by id, find it
          action = getAction(nextActionId);
        }

      } else {
        // otherwise, increment the index
        action = actionList.get(currentIndex + 1);
      }
    } catch (IndexOutOfBoundsException e) {
      // at end of script, fall through to return null nextAction
    }

    return action;
  }

  @Nullable
  public OnboardingConfig getConfig() {
    return config;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WizardScript) {
      WizardScript o = (WizardScript) obj;
      return Objects.equals(scriptUri, o.scriptUri)
          && (firstActionIndex == o.firstActionIndex)
          && Objects.equals(actions, o.actions)
          && Objects.equals(actionList, o.actionList)
          && (version == o.version)
          && Objects.equals(config, o.config);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(scriptUri, firstActionIndex, actionList, version, config);
  }

  @Override
  public String toString() {
    return "WizardScript{scriptUri="
        + scriptUri
        + " firstActionIndex="
        + firstActionIndex
        + " actionList="
        + actionList
        + " version="
        + version
        + " config="
        + config;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(scriptUri);
    dest.writeInt(firstActionIndex);
    dest.writeTypedList(actionList);
    dest.writeInt(version);
    dest.writeParcelable(config, flags);
  }

  /**
   * Return an {@link Iterator} object to traverse the {@link WizardAction} from this {@link
   * WizardScript}.
   */
  @NonNull
  @Override
  public Iterator<WizardAction> iterator() {
    return actionList.iterator();
  }

  public static final Parcelable.Creator<WizardScript> CREATOR =
      new Parcelable.Creator<WizardScript>() {
        @Override
        public WizardScript createFromParcel(Parcel source) {
          String scriptUri = source.readString();
          int firstActionIndex = source.readInt();

          ArrayList<WizardAction> actionList = new ArrayList<>();
          source.readTypedList(actionList, WizardAction.CREATOR);
          int version = source.readInt();
          OnboardingConfig config = source.readParcelable(OnboardingConfig.class.getClassLoader());
          return new WizardScript(scriptUri, actionList, firstActionIndex, version, config);
        }

        @Override
        public WizardScript[] newArray(int size) {
          return new WizardScript[size];
        }
      };

  @NonNull
  @Override
  public PersistableBundle toPersistableBundle() {
    PersistableBundle bundle = new PersistableBundle();
    bundle.putString(SCRIPT_URI_KEY, scriptUri);
    putPersistableList(bundle, ACTION_LIST_KEY, actionList);
    bundle.putInt(FIRST_ACTION_INDEX_KEY, firstActionIndex);
    bundle.putInt(VERSION_KEY, version);
    putPersistable(bundle, CONFIG_KEY, config);
    return bundle;
  }
}
