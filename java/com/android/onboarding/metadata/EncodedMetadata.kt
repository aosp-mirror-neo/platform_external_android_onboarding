package com.android.onboarding.metadata

import com.google.errorprone.annotations.CompileTimeConstant
import java.math.BigDecimal
import java.math.MathContext

/**
 * Encoded Metadata that can be attached to Onboarding nodes.
 *
 * @property paramKey a name that describes this piece of metadata as a compile-time String.
 * @property value the value of this metadata. It is recommended to smart cast [EncodedMetadata] to
 *   one of its subclasses before using [value] for type safety.
 */
sealed interface EncodedMetadata {
  val paramKey: NonSensitiveString
  val value: Any

  /** An [EncodedMetadata] including a Boolean. */
  data class BooleanMetadata(
    override val paramKey: NonSensitiveString,
    override val value: Boolean,
  ) : EncodedMetadata

  /** An [EncodedMetadata] including a value from an enum. */
  data class EnumMetadata<T : Enum<T>>(
    override val paramKey: NonSensitiveString,
    override val value: Enum<T>,
  ) : EncodedMetadata

  /**
   * An [EncodedMetadata] including an Int value.
   *
   * The int value will only be stored to three significant figures.
   */
  data class ApproximateIntMetadata(
    override val paramKey: NonSensitiveString,
    private val intValue: Int,
  ) : EncodedMetadata {
    override val value: Long = BigDecimal(intValue).round(MathContext(3)).longValueExact()
  }

  /** An [EncodedMetadata] including a non-privacy-sensitive string. */
  data class NonSensitiveStringMetadata(
    override val paramKey: NonSensitiveString,
    override val value: NonSensitiveString,
  ) : EncodedMetadata

  /** An [EncodedMetadata] including a string which has explicit privacy approval. */
  data class PrivacyApprovedStringMetadata(
    override val paramKey: NonSensitiveString,
    override val value: String,
    val approvalReference: ApprovalReference,
  ) : EncodedMetadata
}

/** A reference to a formal privacy approval. */
sealed interface ApprovalReference {
  /**
   * A reference to a document showing privacy approval.
   *
   * @param documentUrl The URL of the document showing the relevant privacy approval.
   */
  data class PrivacyApprovalDocument(@CompileTimeConstant private val documentUrl: String) :
    ApprovalReference

  /**
   * A reference to a buganizer bug showing privacy approval.
   *
   * @param bugId A valid buganizer id showing the relevant privacy approval.
   * @param commentId Optionally a reference to the specific comment on the bug showing approval
   */
  data class PrivacyApprovalBug
  @JvmOverloads
  constructor(
    @CompileTimeConstant private val bugId: Long,
    @CompileTimeConstant private val commentId: Long? = null,
  ) : ApprovalReference
}
