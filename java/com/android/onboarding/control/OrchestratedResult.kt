package com.android.onboarding.control

import android.app.Activity
import android.os.PersistableBundle
import androidx.activity.result.ActivityResult
import androidx.annotation.VisibleForTesting
import com.android.onboarding.compatibility.compatGetBoolean
import com.android.onboarding.compatibility.compatPutBoolean
import com.android.onboarding.control.chained.ChainedContract
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.utils.OverriddenEquality
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.UnknownEquality
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.Intent
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.PersistablePersistableBundle
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import com.android.onboarding.utils.persistable.toBundle
import com.android.onboarding.utils.persistable.toPersistableBundle

/** Result to be used with [OrchestratedTask]. */
sealed class OrchestratedResult<out O : Persistable> : Persistable, OverriddenEquality {
  /** Write this orchestrated result to [ActivityResult]. */
  fun toActivityResult(): ActivityResult =
    ActivityResult(Activity.RESULT_OK, Intent { putExtras(toPersistableBundle().toBundle()) })

  /** An [OrchestratedResult] which actually contains a final result. */
  data class Result<out O : Persistable>(val result: O) : OrchestratedResult<O>(), UnknownEquality {
    override fun toPersistableBundle(): PersistableBundle =
      PersistableBundle().apply {
        putString(KEY_RESULT_TYPE, RESULT_TYPE_RESULT)
        putPersistable(RESULT_KEY, result)
      }

    companion object {
      private const val RESULT_KEY = "result"

      /**
       * Creates the [PersistableDeserializer] for an [OrchestratedResult] containing the given
       * result type.
       */
      fun <O : Persistable> deserializer(
        deserializer: PersistableDeserializer<O>
      ): PersistableDeserializer<OrchestratedResult<O>> = Deserializer(deserializer)
    }

    private class Deserializer<O : Persistable>(
      private val deserializer: PersistableDeserializer<O>
    ) : PersistableDeserializer<OrchestratedResult<O>> {
      override fun fromPersistableBundle(
        persistableBundle: PersistableBundle
      ): OrchestratedResult<O> {
        require(persistableBundle.getString(KEY_RESULT_TYPE) == RESULT_TYPE_RESULT)
        return Result(
          result =
            requireNotNull(persistableBundle.getPersistable(RESULT_KEY, deserializer)) {
              "Missing result under key $RESULT_KEY in $persistableBundle"
            }
        )
      }
    }
  }

  /**
   * An [OrchestratedResult] which indicates that the Orchestrator should launch an activity.
   *
   * In response to this result, orchestrators should launch the given [activity] and, if a result
   * is expected, use the result of that activity in place of the otherwise expected result from the
   * task which returned this result.
   */
  data class StartActivity(val activity: PendingContract.ErasedActivity) :
    OrchestratedResult<Nothing>(), StableEquality {
    override fun toPersistableBundle(): PersistableBundle =
      PersistableBundle().apply {
        putString(KEY_RESULT_TYPE, RESULT_TYPE_START_ACTIVITY)
        putPersistable(KEY_PENDING_CONTRACT, activity)
      }

    companion object : PersistableDeserializer<StartActivity> {

      override fun fromPersistableBundle(persistableBundle: PersistableBundle): StartActivity {
        require(persistableBundle.getString(KEY_RESULT_TYPE) == RESULT_TYPE_START_ACTIVITY)
        return StartActivity(
          activity =
            requireNotNull(
              persistableBundle.getPersistable(KEY_PENDING_CONTRACT, PendingContract.Activity)
            ) {
              "Missing activity"
            }
        )
      }
    }
  }

  /**
   * An [OrchestratedResult] which indicates that the Orchestrator should launch an [activity] for
   * result as specified by [activity.target][ChainedContract.Activity.target].
   *
   * In response to this result, orchestrators should launch the given [activity] and, if a result
   * is expected, use the result of that activity to generate a callback via
   * [activity.onResult][ChainedContract.onResult] and then launch it for the final result to be
   * used instead of the result of the node which produced this [OrchestratedResult].
   */
  data class StartActivityForResult(
    val orchestrated: Boolean,
    val activity: ChainedContract.Activity,
  ) : OrchestratedResult<Nothing>(), StableEquality {
    override fun toPersistableBundle(): PersistableBundle =
      PersistableBundle().apply {
        putString(KEY_RESULT_TYPE, RESULT_TYPE_START_ACTIVITY_FOR_RESULT)
        compatPutBoolean(KEY_ORCHESTRATED, orchestrated)
        putPersistable(KEY_PENDING_CONTRACT, activity)
      }

    companion object : PersistableDeserializer<StartActivityForResult> {
      override fun fromPersistableBundle(
        persistableBundle: PersistableBundle
      ): StartActivityForResult {
        require(
          persistableBundle.getString(KEY_RESULT_TYPE) == RESULT_TYPE_START_ACTIVITY_FOR_RESULT
        )
        return StartActivityForResult(
          orchestrated =
            requireNotNull(persistableBundle.compatGetBoolean(KEY_ORCHESTRATED)) {
              "Missing orchestrated"
            },
          activity =
            requireNotNull(
              persistableBundle.getPersistable(KEY_PENDING_CONTRACT, ChainedContract.Activity)
            ) {
              "Missing activity"
            },
        )
      }
    }
  }

  /**
   * An [OrchestratedResult] which indicates that the Orchestrator should launch a [task] for result
   * as specified by [task.target][ChainedContract.Task.target].
   *
   * In response to this result, orchestrators should launch the given [task] and, if a result is
   * expected, use the result of that task to generate a callback via
   * [task.onResult][ChainedContract.onResult] and then launch it for the final result to be used
   * instead of the result of the node which produced this [OrchestratedResult].
   *
   * @property orchestrated does the [task] require orchestration.
   */
  data class StartTaskForResult(val orchestrated: Boolean, val task: ChainedContract.Task) :
    OrchestratedResult<Nothing>(), StableEquality {
    override fun toPersistableBundle(): PersistableBundle =
      PersistableBundle().apply {
        putString(KEY_RESULT_TYPE, RESULT_TYPE_START_TASK_FOR_RESULT)
        compatPutBoolean(KEY_ORCHESTRATED, orchestrated)
        putPersistable(KEY_PENDING_CONTRACT, task)
      }

    companion object : PersistableDeserializer<StartTaskForResult> {
      override fun fromPersistableBundle(persistableBundle: PersistableBundle): StartTaskForResult {
        require(persistableBundle.getString(KEY_RESULT_TYPE) == RESULT_TYPE_START_TASK_FOR_RESULT)
        return StartTaskForResult(
          orchestrated =
            requireNotNull(persistableBundle.compatGetBoolean(KEY_ORCHESTRATED)) {
              "Missing orchestrated"
            },
          task =
            requireNotNull(
              persistableBundle.getPersistable(KEY_PENDING_CONTRACT, ChainedContract.Task)
            ) {
              "Missing task"
            },
        )
      }
    }
  }

  /** A deserializer for [OrchestratedResult]. */
  interface Deserializer<O : Persistable> : PersistableDeserializer<OrchestratedResult<O>> {
    /** Read the [OrchestratedResult] from the [ActivityResult]. */
    fun fromActivityResult(result: ActivityResult): OrchestratedResult<O>
  }

  private class DeserializerImpl<O : Persistable>(
    private val deserializer: PersistableDeserializer<O>
  ) : Deserializer<O> {
    /** Read the [OrchestratedResult] from the [ActivityResult]. */
    override fun fromActivityResult(result: ActivityResult): OrchestratedResult<O> =
      fromPersistableBundle(result.data?.extras?.toPersistableBundle() ?: PersistableBundle())

    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): OrchestratedResult<O> =
      when (persistableBundle.getString(KEY_RESULT_TYPE)) {
        RESULT_TYPE_RESULT ->
          Result.deserializer(deserializer).fromPersistableBundle(persistableBundle)
        RESULT_TYPE_START_ACTIVITY -> StartActivity.fromPersistableBundle(persistableBundle)
        RESULT_TYPE_START_ACTIVITY_FOR_RESULT ->
          StartActivityForResult.fromPersistableBundle(persistableBundle)
        RESULT_TYPE_START_TASK_FOR_RESULT ->
          StartTaskForResult.fromPersistableBundle(persistableBundle)
        else -> {
          Log.w(TAG, "No $KEY_RESULT_TYPE extra - assuming raw result type.")
          Result(deserializer.fromPersistableBundle(persistableBundle))
        }
      }
  }

  companion object :
    Deserializer<PersistablePersistableBundle> by DeserializerImpl({
      PersistablePersistableBundle(it)
    }) {
    private const val TAG = "OrchestratedResult"
    private const val KEY_PENDING_CONTRACT = "contract"
    private const val KEY_ORCHESTRATED = "orchestrated"
    @VisibleForTesting const val KEY_RESULT_TYPE = "resultType"
    private const val RESULT_TYPE_RESULT = "result"
    private const val RESULT_TYPE_START_ACTIVITY = "startActivity"
    private const val RESULT_TYPE_START_ACTIVITY_FOR_RESULT = "startActivityForResult"
    private const val RESULT_TYPE_START_TASK_FOR_RESULT = "startTaskForResult"

    /**
     * Creates the [PersistableDeserializer] for an [OrchestratedResult] containing the given result
     * type.
     */
    fun <O : Persistable> deserializer(deserializer: PersistableDeserializer<O>): Deserializer<O> =
      DeserializerImpl(deserializer)
  }
}
