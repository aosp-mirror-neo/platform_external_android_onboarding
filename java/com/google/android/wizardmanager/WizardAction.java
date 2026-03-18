package com.google.android.wizardmanager;

import static com.android.onboarding.utils.persistable.PersistableBundleKt.putPersistable;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.android.onboarding.config.OnboardingConfig;
import com.android.onboarding.utils.StableEquality;
import com.android.onboarding.utils.persistable.Persistable;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
  static final String DEPENDENCIES_KEY = "dependencies";
  static final String CONFIG_KEY = "config";

  /** Special action id indicating that the script should be yielded. */
  public static final String YIELD_ACTION_ID = "#YIELD";

  /** The yield action that is used to yield the script. */
  public static final WizardAction YIELD_ACTION =
      new WizardAction.Builder(YIELD_ACTION_ID, 0).id(YIELD_ACTION_ID).uri(null).build();

  /** Copied from `WizardManager.EXTRA_LOAD_SCRIPT_URI`. */
  private static final String EXTRA_LOAD_SCRIPT_URI = "loadScriptUri";

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

  /** The dependencies of this action. */
  @NonNull private final List<String> dependencies;

  /** The URI of the script declaring this action. */
  @NonNull private final String scriptUri;

  /** The ifLifecycle of the script declaring this action. */
  @Nullable private final String ifLifecycle;

  /** The customized flow of the script declaring this customized scriptUri. */
  @Nullable private final String flow;

  /** The ifFlow of the script declaring this action. */
  @Nullable private final String ifFlow;

  @NonNull private final String logId;

  /** The WizardScript config applied for this action. */
  @Nullable private final OnboardingConfig config;

  protected WizardAction(Builder builder) {
    this.scriptUri = builder.scriptUri;
    this.index = builder.index;
    this.id = builder.id;
    this.uri = builder.uri;
    this.node = builder.node;
    this.ifLifecycle = builder.ifLifecycle;
    this.flow = builder.flow;
    this.ifFlow = builder.ifFlow;
    this.branches = builder.branches;
    this.dependencies = builder.dependencies;
    this.logId = String.format("%s#%s", scriptUri, id);
    this.config = builder.config;
  }

  /** Builder for {@link WizardAction}. */
  public static class Builder {
    private final String scriptUri;
    private final int index;
    private String id;
    private String uri;
    private String node;
    private String ifLifecycle;
    private String flow;
    private String ifFlow;
    private WizardBranchArray branches = new WizardBranchArray();
    private List<String> dependencies = new ArrayList<>();
    @Nullable private OnboardingConfig config;

    public Builder(@NonNull String scriptUri, int index) {
      this.scriptUri = scriptUri;
      this.index = index;
    }

    @CanIgnoreReturnValue
    public Builder id(@Nullable String id) {
      this.id = id;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder uri(@Nullable String uri) {
      this.uri = uri;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder node(@Nullable String node) {
      this.node = node;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder ifLifecycle(@Nullable String ifLifecycle) {
      this.ifLifecycle = ifLifecycle;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder flow(@Nullable String flow) {
      this.flow = flow;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder ifFlow(@Nullable String ifFlow) {
      this.ifFlow = ifFlow;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder branches(@NonNull WizardBranchArray branches) {
      this.branches = branches;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder dependencies(@NonNull List<String> dependencies) {
      this.dependencies = dependencies;
      return this;
    }

    @CanIgnoreReturnValue
    public Builder config(@Nullable OnboardingConfig config) {
      this.config = config;
      return this;
    }

    public WizardAction build() {
      return new WizardAction(this);
    }
  }

  public Builder toBuilder() {
    return new Builder(scriptUri, index)
        .id(id)
        .uri(uri)
        .node(node)
        .ifLifecycle(ifLifecycle)
        .flow(flow)
        .ifFlow(ifFlow)
        .branches(branches)
        .dependencies(dependencies)
        .config(config);
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

  @NonNull
  public List<String> getDependencies() {
    return dependencies;
  }

  @Nullable
  public OnboardingConfig getConfig() {
    return config;
  }

  /** Returns the URI of the script to execute for this action. */
  @VisibleForTesting
  @Nullable
  public String getLoadScriptUri() {
    // We reverse engineer WizardActionParser here. We decided not to store the loadScriptUri
    // attribute with another persistable key to keep the WizardAction object lean.
    if (TextUtils.isEmpty(uri) || !uri.startsWith("intent:")) {
      return null;
    }
    try {
      return Intent.parseUri(uri, 0).getStringExtra(EXTRA_LOAD_SCRIPT_URI);
    } catch (URISyntaxException e) {
      WzmLog.w("Failed to get loadScriptUri from uri", e);
    }
    return null;
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
          && Objects.equals(ifFlow, o.ifFlow)
          && Objects.equals(dependencies, o.dependencies)
          && Objects.equals(config, o.config);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        scriptUri, index, id, uri, node, ifLifecycle, flow, ifFlow, branches, dependencies, config);
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
        + " dependencies="
        + dependencies
        + " config="
        + config
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
    bldr.append(indentStr).append(" dependencies = ").append(dependencies).append("\n");
    bldr.append(indentStr).append(" config = ").append(config).append("\n");
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
    dest.writeStringList(dependencies);
    dest.writeParcelable(config, flags);
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
          List<String> dependencies = new ArrayList<>();
          source.readStringList(dependencies);
          OnboardingConfig config = source.readParcelable(OnboardingConfig.class.getClassLoader());
          return new WizardAction.Builder(scriptUri, index)
              .id(id)
              .uri(uri)
              .node(node)
              .ifLifecycle(ifLifecycle)
              .flow(flow)
              .ifFlow(ifFlow)
              .branches(branches)
              .dependencies(dependencies)
              .config(config)
              .build();
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
      WzmLog.d(prefix + "config=" + instance.config);
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
    bundle.putStringArray(DEPENDENCIES_KEY, dependencies.toArray(new String[0]));
    putPersistable(bundle, CONFIG_KEY, config);
    return bundle;
  }
}
