package com.android.onboarding.tasks.service

import android.os.PersistableBundle
import com.android.onboarding.nodes.UniqueNodeRef
import com.android.onboarding.nodes.generateHash
import com.android.onboarding.nodes.refersTo
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.PersistableDeserializer
import java.util.Objects
import org.apache.commons.lang3.SerializationUtils

/** A [Persistable] [Exception] which can be returned by the [OnboardingTaskService]. */
sealed class OnboardingTaskServiceException(
  ref: UniqueNodeRef,
  override val message: String,
  override val cause: Throwable,
  private val type: String,
) :
  Exception(message, cause),
  Persistable,
  StableEquality,
  UniqueNodeRef.Stable by ref.asUniqueNodeRef() {
  override fun toPersistableBundle() =
    UniqueNodeRef.toPersistableBundle(this).apply {
      val serialisedCause =
        try {
          SerializationUtils.serialize(cause)
        } catch (e: Throwable) {
          SerializationUtils.serialize(
            IllegalStateException("Cause (${cause.message}) serialisation failed: ${e.message}")
          )
        }
      putString(KEY_MESSAGE, message)
      putIntArray(KEY_CAUSE, serialisedCause.map(Byte::toInt).toIntArray())
      putString(KEY_TYPE, type)
    }

  override fun getLocalizedMessage(): String = "[${identity()}] ${super.getLocalizedMessage()}"

  override fun hashCode() = Objects.hash(message, cause.stackTraceToString(), type, generateHash())

  override fun equals(other: Any?) =
    other is OnboardingTaskServiceException &&
      other.message == message &&
      other.cause.stackTraceToString() == cause.stackTraceToString() &&
      other.type == type &&
      other refersTo this

  companion object : PersistableDeserializer<OnboardingTaskServiceException> {
    private const val KEY_MESSAGE = "MESSAGE"
    private const val KEY_CAUSE = "CAUSE"

    /** Read [error] and build [OnboardingTaskServiceServerException] from it. */
    suspend fun fromPersistableBundle(
      error: IPersistableBundleStream
    ): OnboardingTaskServiceException = fromPersistableBundle(error.await())

    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): OnboardingTaskServiceException {
      val message = requireNotNull(persistableBundle.getString(KEY_MESSAGE))
      val cause: Throwable =
        requireNotNull(persistableBundle.getIntArray(KEY_CAUSE))
          .map(Int::toByte)
          .toByteArray()
          .runCatching { SerializationUtils.deserialize<Throwable>(this) }
          .getOrElse { IllegalStateException("Cause deserialisation failed: ${it.message}") }
      val ref = requireNotNull(UniqueNodeRef.fromPersistableBundle(persistableBundle))
      return when (val type = persistableBundle.getString(KEY_TYPE)) {
        TYPE_CLIENT ->
          OnboardingTaskServiceClientException(ref = ref, message = message, cause = cause)
        TYPE_SERVER ->
          OnboardingTaskServiceServerException(ref = ref, message = message, cause = cause)
        else -> error("Unknown OnboardingTaskServiceException($type)")
      }
    }
  }
}

private const val KEY_TYPE = "TYPE"
private const val TYPE_CLIENT = "CLIENT"
private const val TYPE_SERVER = "SERVER"

/**
 * [OnboardingTaskServiceException] which has originated on the server. This usually indicates
 * failures during server method execution after making successful connection.
 */
class OnboardingTaskServiceServerException(
  ref: UniqueNodeRef,
  cause: Throwable,
  message: String? = null,
) :
  OnboardingTaskServiceException(
    ref = ref,
    message =
      message
        ?: cause.message
        ?: "Unknown onboarding task service server error ${cause::class.simpleName}.",
    cause = cause,
    type = TYPE_SERVER,
  )

/**
 * [OnboardingTaskServiceException] which has originated on the client. This usually indicates
 * failures happening before the server was reached.
 */
class OnboardingTaskServiceClientException(
  ref: UniqueNodeRef,
  cause: Throwable,
  message: String? = null,
) :
  OnboardingTaskServiceException(
    ref = ref,
    message =
      message
        ?: cause.message
        ?: "Unknown onboarding task service client error ${cause::class.simpleName}.",
    cause = cause,
    type = TYPE_CLIENT,
  )
