package com.android.onboarding.tasks.precalculation

import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistableEnum

/**
 * The trigger options for a [PrecalculationTask]. For use with the
 * [Precalculation][com.android.onboarding.tasks.precalculation.annotations.Precalculation]
 * annotation.
 */
enum class PrecalculationTrigger : PersistableEnum {
  UNKNOWN,
  PHENOTYPE_SYNC_COMPLETE,
  PLAY_EARLY_UPDATE_COMPLETE,
  AUTH_EARLY_UPDATE_COMPLETE,
  /** An account has been added to the device. Will only trigger after an account has been added. */
  FIRST_ACCOUNT_ADDED,
  /**
   * The point in setup where an account may be added has completed. Will trigger regardless of
   * whether an account was added.
   */
  ACCOUNT_SETUP_COMPLETE,
  PERSONALIZATION_FLOW_STARTED,
  /** Triggered at the very beginning of deferred setup. */
  DEFERRED_SETUP_STARTED;

  override val id = name

  companion object :
    PersistableDeserializer<PrecalculationTrigger> by PersistableEnum.deserializer(
      entries,
      UNKNOWN,
    )
}
