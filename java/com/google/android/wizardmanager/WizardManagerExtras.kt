package com.google.android.wizardmanager

import android.content.Intent
import android.os.BaseBundle
import android.os.Bundle
import android.os.PersistableBundle
import com.android.onboarding.metadata.OnboardingMetadataStore
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.getPersistableMapWithLongKey
import com.android.onboarding.utils.persistable.putPersistable
import com.android.onboarding.utils.persistable.putPersistableMapWithLongKey
import com.google.android.setupcompat.util.SuwLifeCycleEnum
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_IS_DEFERRED_SETUP
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_IS_FIRST_RUN
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_IS_PORTAL_SETUP
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_IS_PRE_DEFERRED_SETUP
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_IS_SETUP_FLOW
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_PENDING_ACTIVITY_METADATA
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_SUW_LIFECYCLE
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_THEME
import com.google.android.setupcompat.util.WizardManagerConstants.EXTRA_WIZARD_BUNDLE
import com.google.errorprone.annotations.CanIgnoreReturnValue


/**
 * Contains contextual information used by Wizard Manager and by screens shown during Onboarding.
 *
 * This data is expected to be passed forward through the entire onboarding flow.
 *
 * Note that there is a [Bundle] representation and a [PersistableBundle] representation. These are
 * not identical and you should not convert between them without going via the [WizardManagerExtras]
 * class. That is, `WizardManagerExtras.fromPersistableBundle(persistableBundle).toBundle()` is a
 * valid conversion but `Bundle(persistableBundle)` is not.
 */
data class WizardManagerExtras(
  val wizardBundle: WizardBundle,
  val setupWizardExtras: SetupWizardExtras = SetupWizardExtras.EMPTY,
  val pendingActivityMetadata: Map<Long, OnboardingMetadataStore> = mapOf(),
) : Persistable, StableEquality {

  /**
   * A builder used for Java clients.
   *
   * Kotlin clients should use [WizardManagerExtras.copy] instead.
   */
  class Builder internal constructor(original: WizardManagerExtras) {
    private var copy = original.copy()

    fun pendingActivityMetadata(
      pendingActivityMetadata: Map<Long, OnboardingMetadataStore>
    ): Builder {
      copy = copy.copy(pendingActivityMetadata = pendingActivityMetadata)
      return this
    }

    fun build() = copy
  }

  /**
   * Create a new [Builder] to create a [WizardManagerExtras] with values modified from this
   * instance.
   *
   * Kotlin callers should use [copy] instead.
   */
  fun builder() = Builder(this)

  /**
   * Writes these extras to a [Bundle].
   *
   * This representation is identical to the one used by [WizardManagerHelper].
   */
  fun writeToBundle(bundle: Bundle) {
    bundle.putBundle(EXTRA_WIZARD_BUNDLE, wizardBundle.toBundle())
    bundle.putPersistableMapWithLongKey(EXTRA_PENDING_ACTIVITY_METADATA, pendingActivityMetadata)
    setupWizardExtras.writeToBaseBundle(bundle)
  }

  /** Creates a new [Bundle] containing only the wizard manager extras. */
  fun toBundle(): Bundle = Bundle().also { writeToBundle(it) }

  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().also {
      it.putPersistable(EXTRA_WIZARD_BUNDLE, wizardBundle)
      it.putPersistableMapWithLongKey(EXTRA_PENDING_ACTIVITY_METADATA, pendingActivityMetadata)
      setupWizardExtras.writeToBaseBundle(it)
    }

  companion object : PersistableDeserializer<WizardManagerExtras> {

    /**
     * Reads the data from a [Bundle].
     *
     * If [EXTRA_WIZARD_BUNDLE] is not specified then an exception will be thrown. Any other missing
     * keys will be set to a default value of false, null, or unknown.
     */
    fun fromBundle(bundle: Bundle): WizardManagerExtras {
      return WizardManagerExtras(
        wizardBundle =
          WizardBundle.fromBundle(
            bundle.getBundle(EXTRA_WIZARD_BUNDLE) ?: error("Missing Wizard Bundle")
          ),
        setupWizardExtras = SetupWizardExtras.fromBundle(bundle),
        pendingActivityMetadata =
          bundle.getPersistableMapWithLongKey(
            EXTRA_PENDING_ACTIVITY_METADATA,
            OnboardingMetadataStore,
          ) ?: mapOf(),
      )
    }

    /**
     * Reads the data from a [PersistableBundle].
     *
     * If [EXTRA_WIZARD_BUNDLE] is not specified then an exception will be thrown. Any other missing
     * keys will be set to a default value of false, null, or unknown.
     */
    override fun fromPersistableBundle(persistableBundle: PersistableBundle): WizardManagerExtras {
      return WizardManagerExtras(
        wizardBundle =
          persistableBundle.getPersistable(EXTRA_WIZARD_BUNDLE, WizardBundle)
            ?: error("Missing WizardBundle"),
        setupWizardExtras = SetupWizardExtras.fromPersistableBundle(persistableBundle),
        pendingActivityMetadata =
          persistableBundle.getPersistableMapWithLongKey(
            EXTRA_PENDING_ACTIVITY_METADATA,
            OnboardingMetadataStore,
          ) ?: mapOf(),
      )
    }
  }
}

/**
 * Contains contextual information used by screens shown during Onboarding.
 *
 * This data is expected to be passed forward through the entire onboarding flow.
 */
data class SetupWizardExtras(
  val isFirstRun: Boolean = false,
  val isDeferredSetup: Boolean = false,
  val isPreDeferredSetup: Boolean = false,
  val isPortalSetup: Boolean = false,
  val isSetupFlow: Boolean = false,
  val isSuwSuggestedActionFlow: Boolean = false,
  val suwLifecycle: SuwLifeCycleEnum = SuwLifeCycleEnum.UNKNOWN,
  val theme: String? = null,
) : Persistable, StableEquality {

  /** Creates a new [Bundle] containing only the setup design library extras. */
  fun toBundle(): Bundle = Bundle().also { writeToBaseBundle(it) }

  override fun toPersistableBundle(): PersistableBundle =
    PersistableBundle().also { writeToBaseBundle(it) }

  /** Writes data common between the [PersistableBundle] and [Bundle] representations. */
  internal fun writeToBaseBundle(bundle: BaseBundle) {
    bundle.putBoolean(EXTRA_IS_FIRST_RUN, isFirstRun)
    bundle.putBoolean(EXTRA_IS_DEFERRED_SETUP, isDeferredSetup)
    bundle.putBoolean(EXTRA_IS_PRE_DEFERRED_SETUP, isPreDeferredSetup)
    bundle.putBoolean(EXTRA_IS_PORTAL_SETUP, isPortalSetup)
    bundle.putBoolean(EXTRA_IS_SETUP_FLOW, isSetupFlow)
    bundle.putBoolean(EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, isSuwSuggestedActionFlow)
    bundle.putInt(EXTRA_SUW_LIFECYCLE, suwLifecycle.value)
    bundle.putString(EXTRA_THEME, theme)
  }

  companion object : PersistableDeserializer<SetupWizardExtras> {
    /** Default [SetupWizardExtras] values, equivalent to not specifying extras. */
    val EMPTY =
      SetupWizardExtras(
        isFirstRun = false,
        isDeferredSetup = false,
        isPreDeferredSetup = false,
        isPortalSetup = false,
        isSetupFlow = false,
        isSuwSuggestedActionFlow = false,
        suwLifecycle = SuwLifeCycleEnum.UNKNOWN,
        theme = null,
      )

    /**
     * Reads the data from a [Bundle].
     *
     * Any missing keys will be set to a default value of false, null, or unknown.
     */
    fun fromBundle(bundle: BaseBundle): SetupWizardExtras {
      val suwLifecycleValue = bundle.getInt(EXTRA_SUW_LIFECYCLE)
      return SetupWizardExtras(
        isFirstRun = bundle.getBoolean(EXTRA_IS_FIRST_RUN),
        isDeferredSetup = bundle.getBoolean(EXTRA_IS_DEFERRED_SETUP),
        isPreDeferredSetup = bundle.getBoolean(EXTRA_IS_PRE_DEFERRED_SETUP),
        isPortalSetup = bundle.getBoolean(EXTRA_IS_PORTAL_SETUP),
        isSetupFlow = bundle.getBoolean(EXTRA_IS_SETUP_FLOW),
        isSuwSuggestedActionFlow = bundle.getBoolean(EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW),
        suwLifecycle =
          SuwLifeCycleEnum.entries.firstOrNull { it.value == suwLifecycleValue }
            ?: SuwLifeCycleEnum.UNKNOWN,
        theme = bundle.getString(EXTRA_THEME),
      )
    }

    /**
     * Reads the data from a [PersistableBundle].
     *
     * Any missing keys will be set to a default value of false, null, or unknown.
     */
    override fun fromPersistableBundle(persistableBundle: PersistableBundle): SetupWizardExtras =
      fromBundle(persistableBundle)
  }
}



/**
 * Fetches [WizardManagerExtras] from the [Bundle].
 *
 * If [EXTRA_WIZARD_BUNDLE] is not specified then an exception will be thrown. Any other missing
 * keys will be set to a default value of false, null, or unknown.
 */
val Bundle.wizardManagerExtras: WizardManagerExtras
  get() = WizardManagerExtras.fromBundle(this)

/** See [WizardManagerExtras.writeToBundle] */
fun Bundle.putWizardManagerExtras(wizardManagerExtras: WizardManagerExtras) {
  wizardManagerExtras.writeToBundle(this)
}

/**
 * Fetches [WizardManagerExtras] from the [Intent].
 *
 * If [EXTRA_WIZARD_BUNDLE] is not specified then an exception will be thrown. Any other missing
 * keys will be set to a default value of false, null, or unknown.
 */
val Intent.wizardManagerExtras
  get() =
    extras?.wizardManagerExtras ?: error("Tried to extract WizardManagerExtras but no extras exist")

/** See [WizardManagerExtras.writeToBundle] */
fun Intent.putWizardManagerExtras(wizardManagerExtras: WizardManagerExtras) {
  putExtras(wizardManagerExtras.toBundle())
}

/**
 * Fetches [SetupWizardExtras] from the [Bundle].
 *
 * Any missing keys will be set to a default value of false, null, or unknown.
 */
val Bundle.setupWizardExtras: SetupWizardExtras
  get() = SetupWizardExtras.fromBundle(this)

/** See [SetupWizardExtras.writeToBaseBundle] */
fun Bundle.putSetupWizardExtras(setupWizardExtras: SetupWizardExtras) {
  setupWizardExtras.writeToBaseBundle(this)
}

/**
 * Fetches [SetupWizardExtras] from the [Intent].
 *
 * Any missing keys will be set to a default value of false, null, or unknown.
 */
val Intent.setupWizardExtras
  get() = extras?.setupWizardExtras ?: SetupWizardExtras.EMPTY

/** See [SetupWizardExtras.writeToBaseBundle] */
@CanIgnoreReturnValue
fun Intent.putSetupWizardExtras(setupWizardExtras: SetupWizardExtras): Intent {
  putExtras(setupWizardExtras.toBundle())
  return this
}
