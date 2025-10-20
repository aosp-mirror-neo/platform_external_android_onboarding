package com.android.onboarding.utils.persistable

import android.app.Activity
import android.os.PersistableBundle
import androidx.activity.result.ActivityResult
import com.android.onboarding.utils.StableEquality
import java.util.Objects

/**
 * A wrapper around [ActivityResult] to allow persistence to disk.
 *
 * @property activityResult denotes the result of an activity start.
 *
 * Note that it uses [ActivityResultPersistableDelegate] to serialise [ActivityResult] and as such
 * is subject to all of its limitations.
 *
 * We need this in addition to the [ActivityResultPersistableDelegate] as some class expect
 * Persistable type and as such PersistableDelegate won't work.
 */
data class PersistableActivityResult(val activityResult: ActivityResult) :
  Persistable, StableEquality {

  override fun equals(other: Any?) =
    other is PersistableActivityResult &&
      activityResult.resultCode == other.activityResult.resultCode &&
      activityResult.data?.let(::PersistableIntent) ==
        other.activityResult.data?.let(::PersistableIntent)

  override fun hashCode() =
    Objects.hash(activityResult.resultCode, activityResult.data?.let(::PersistableIntent))

  override fun toPersistableBundle() =
    ActivityResultPersistableDelegate.toPersistableBundle(activityResult)

  companion object :
    PersistableDeserializer<PersistableActivityResult>, PersistableSerializer<ActivityResult> {
    override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
      PersistableActivityResult(
        ActivityResultPersistableDelegate.fromPersistableBundle(persistableBundle)
      )

    override fun toPersistableBundle(value: ActivityResult) =
      PersistableActivityResult(value).toPersistableBundle()
  }
}

/**
 * Serialize this [Persistable] to [ActivityResult].
 *
 * Serialization happens at [ActivityResult.data] extras root and matches the properties that would
 * be written to a [PersistableBundle] via [toPersistableBundle][Persistable.toPersistableBundle].
 * Returned [ActivityResult.data] has nothing configured except extras.
 */
fun Persistable.toActivityResult(resultCode: Int = Activity.RESULT_OK): ActivityResult =
  ActivityResult(resultCode, toIntent())

/**
 * [toActivityResult] for [PersistableDeserializer].
 *
 * @see Persistable.toActivityResult
 */
@Suppress("UnusedReceiverParameter", "NOTHING_TO_INLINE")
inline fun <P : Persistable> PersistableDeserializer<P>.toActivityResult(
  value: P,
  resultCode: Int = Activity.RESULT_OK,
): ActivityResult = value.toActivityResult(resultCode)

/**
 * Deserialize [P] from a supplied [result] extras.
 *
 * Assumes [ActivityResult.data] extras were serialized via
 * [toActivityResult][Persistable.toActivityResult].
 */
fun <P> PersistableDeserializer<P>.fromActivityResult(result: ActivityResult): P =
  fromIntent(requireNotNull(result.data))

/**
 * A more strict deserializer for [PersistableActivityResult] that will throw an error if the
 * persistable is not an ActivityResult, otherwise delegate to
 * [PersistableActivityResult.Companion].
 */
object StrictPersistableActivityResultDeserializer :
  PersistableDeserializer<PersistableActivityResult> {
  override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
    persistableBundle
      .takeIf { ActivityResultPersistableDelegate.isActivityResultPersistable(it) }
      ?.let { PersistableActivityResult.fromPersistableBundle(it) }
      ?: throw IllegalArgumentException("Bundle is not a valid ActivityResult persistable.")
}
