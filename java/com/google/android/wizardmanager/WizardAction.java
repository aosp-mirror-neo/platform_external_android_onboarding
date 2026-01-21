package com.google.android.wizardmanager;

import static com.android.onboarding.utils.persistable.PersistableBundleKt.putPersistable;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.android.onboarding.utils.StableEquality;
import com.android.onboarding.utils.persistable.Persistable;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

/**
 * Description of a single WizardAction item contained within a setup script. This is an immutable
 * object.
 */
@Keep
public class WizardAction implements Parcelable, Persistable, StableEquality {

  // These can be made private if converted to Kotlin and the Deserializer moved
  static final String SCRIPT_URI_KEY = "script_uri";
  static final String INDEX_KEY = "index";
  static final String ID_KEY = "id";
  static final String URI_KEY = "uri";
  static final String NODE_KEY = "node";
  static final String IF_LIFECYCLE_KEY = "if_lifecycle";
  static final String FLOW_KEY = "flow";
  static final String IF_FLOW_KEY = "if_flow";
  static final String BRANCHES_KEY = "branches";

  /** Special action id indicating that the script should be yielded. */
  public static final String YIELD_ACTION_ID = "#YIELD";

  /** The yield action that is used to yield the script. */
  public static final WizardAction YIELD_ACTION =
      new WizardAction(
          /* scriptUri= */ YIELD_ACTION_ID,
          /* index= */ 0,
          /* id= */ YIELD_ACTION_ID,
          /* uri= */ null,
          new WizardBranchArray());

  /** URI to be invoked for this action, generated using "adb shell am to-intent-uri" */
  @Nullable private final String uri;

  /** node to be invoked for the contract" */
  @Nullable private final String node;

  /** The index of this action in its script */
  private final int index;

  /** The id for this action */
  @Nullable private final String id;

  /** Transition table, mapping result codes to the next action to run */
  @NonNull private final WizardBranchArray branches;

  /** The URI of the script declaring this action. */
  @NonNull private final String scriptUri;

  /** The ifLifecycle of the script declaring this action. */
  @Nullable private final String ifLifecycle;

  /** The customized flow of the script declaring this customized scriptUri. */
  @Nullable private final String flow;

  /** The ifFlow of the script declaring this action. */
  @Nullable private final String ifFlow;

  @NonNull private final String logId;

  public WizardAction(
      @NonNull String scriptUri,
      int index,
      @Nullable String id,
      @Nullable String uri,
      @NonNull WizardBranchArray branches) {
    this(
        scriptUri,
        index,
        id,
        uri,
        /* node= */ null,
        /* ifLifecycle= */ null,
        /* flow= */ null,
        /* ifFlow= */ null,
        branches);
  }

  protected WizardAction(WizardAction action) {
    this(
        action.scriptUri,
        action.index,
        action.id,
        action.uri,
        action.node,
        action.ifLifecycle,
        action.flow,
        action.ifFlow,
        action.branches);
  }

  public WizardAction(
      @NonNull String scriptUri,
      int index,
      @Nullable String id,
      @Nullable String uri,
      @Nullable String node,
      @Nullable String ifLifecycle,
      @Nullable String flow,
      @Nullable String ifFlow,
      @NonNull WizardBranchArray branches) {
    this.scriptUri = scriptUri;
    this.index = index;
    this.id = id;
    this.uri = uri;
    this.node = node;
    this.ifLifecycle = ifLifecycle;
    this.flow = flow;
    this.ifFlow = ifFlow;
    this.branches = branches;
    this.logId = String.format("%s#%s", scriptUri, id);
  }

  @Nullable
  public Intent getIntent() {
    Intent intent = null;
    if (uri != null) {
      try {
        intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
      } catch (URISyntaxException e) {
        WzmLog.e("getIntent() FAIL due to bad URI: " + uri);
      }
    }
    return intent;
  }

  public int getIndex() {
    return index;
  }

  @Nullable
  public String getId() {
    return id;
  }

  @Nullable
  public String getUri() {
    return uri;
  }

  @Nullable
  public String getNode() {
    return node;
  }

  @NonNull
  public String getScriptUri() {
    return scriptUri;
  }

  @Nullable
  public String getIfLifecycle() {
    return ifLifecycle;
  }

  @Nullable
  public String getCustomizedFlow() {
    return flow;
  }

  @Nullable
  public String getIfFlow() {
    return ifFlow;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public String getNextActionId(int resultCode) {
    return branches.getActionId(resultCode);
  }

  List<String> getNextActionIds(int resultCode) {
    return branches.getActionIds(resultCode);
  }

  /** Returns the unique identifier prefixed with the {@link #scriptUri}. */
  @NonNull
  public String getLogId() {
    return logId;
  }

  @NonNull
  public WizardBranchArray getBranches() {
    return branches;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WizardAction) {
      WizardAction o = (WizardAction) obj;
      return index == o.index
          && Objects.equals(id, o.id)
          && Objects.equals(uri, o.uri)
          && Objects.equals(node, o.node)
          && Objects.equals(branches, o.branches)
          && Objects.equals(scriptUri, o.scriptUri)
          && Objects.equals(ifLifecycle, o.ifLifecycle)
          && Objects.equals(flow, o.flow)
          && Objects.equals(ifFlow, o.ifFlow);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(scriptUri, index, id, uri, node, ifLifecycle, flow, ifFlow, branches);
  }

  @Override
  public String toString() {
    return "WizardAction{id="
        + id
        + " uri="
        + uri
        + " node="
        + node
        + " scriptUri="
        + scriptUri
        + " ifLifecycle="
        + ifLifecycle
        + " flow="
        + flow
        + " ifFlow="
        + ifFlow
        + " index="
        + index
        + " branches="
        + branches
        + "}";
  }

  public String indentedToString() {
    final String indentStr = "  ";
    StringBuilder bldr = new StringBuilder("{\n");
    bldr.append(indentStr).append(" scriptUri = ").append(scriptUri).append("\n");
    bldr.append(indentStr).append(" index = ").append(index).append("\n");
    bldr.append(indentStr).append(" id = ").append(id).append("\n");
    bldr.append(indentStr).append(" uri = ").append(uri).append("\n");
    bldr.append(indentStr).append(" node = ").append(node).append("\n");
    bldr.append(indentStr).append(" ifLifecycle = ").append(ifLifecycle).append("\n");
    bldr.append(indentStr).append(" flow = ").append(flow).append("\n");
    bldr.append(indentStr).append(" ifFlow = ").append(ifFlow).append("\n");
    bldr.append(indentStr).append(" branches = ").append(branches).append("\n");
    return bldr.append(indentStr).append("}").toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(scriptUri);
    dest.writeInt(index);
    dest.writeString(id);
    dest.writeString(uri);
    dest.writeString(node);
    dest.writeString(ifLifecycle);
    dest.writeString(flow);
    dest.writeString(ifFlow);
    dest.writeParcelable(branches, flags);
  }

  public static final Parcelable.Creator<WizardAction> CREATOR =
      new Parcelable.Creator<WizardAction>() {
        @Override
        public WizardAction createFromParcel(Parcel source) {
          String scriptUri = source.readString();
          int index = source.readInt();
          String id = source.readString();
          String uri = source.readString();
          String node = source.readString();
          String ifLifecycle = source.readString();
          String flow = source.readString();
          String ifFlow = source.readString();

          WizardBranchArray branches =
              source.readParcelable(WizardBranchArray.class.getClassLoader());
          return new WizardAction(
              scriptUri, index, id, uri, node, ifLifecycle, flow, ifFlow, branches);
        }

        @Override
        public WizardAction[] newArray(int size) {
          return new WizardAction[size];
        }
      };

  public static void dump(String prefix, WizardAction instance) {
    if (prefix == null) {
      prefix = "";
    }

    if (instance != null) {
      WzmLog.d(prefix + "scriptUri=" + instance.scriptUri);
      WzmLog.d(prefix + "index=" + instance.index);
      WzmLog.d(prefix + "id=" + instance.id);
      WzmLog.d(prefix + "uri=" + instance.uri);
      WzmLog.d(prefix + "node=" + instance.node);
      WzmLog.d(prefix + "ifLifecycle=" + instance.ifLifecycle);
      WzmLog.d(prefix + "flow=" + instance.flow);
      WzmLog.d(prefix + "ifFlow=" + instance.ifFlow);
      WzmLog.d(prefix + "branches=" + instance.branches);
    } else {
      WzmLog.d(prefix + "DUMP WizardAction instance is null");
    }
  }

  @NonNull
  @Override
  public PersistableBundle toPersistableBundle() {
    PersistableBundle bundle = new PersistableBundle();
    bundle.putString(SCRIPT_URI_KEY, scriptUri);
    bundle.putInt(INDEX_KEY, index);
    bundle.putString(ID_KEY, id);
    bundle.putString(URI_KEY, uri);
    bundle.putString(NODE_KEY, node);
    bundle.putString(IF_LIFECYCLE_KEY, ifLifecycle);
    bundle.putString(FLOW_KEY, flow);
    bundle.putString(IF_FLOW_KEY, ifFlow);
    putPersistable(bundle, BRANCHES_KEY, branches);

    return bundle;
  }
}
