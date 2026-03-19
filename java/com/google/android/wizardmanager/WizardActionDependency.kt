package com.google.android.wizardmanager

import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.Persistable
import com.android.onboarding.utils.persistable.putPersistableList
import java.util.Objects

/**
 * Represents a tree of dependencies for a WizardAction.
 *
 * A dependency can be a single action ID, or a logical group (ALL_OF, ANY_OF) containing children.
 */
class WizardActionDependency
private constructor(val type: Type, val actionId: String?, children: List<WizardActionDependency>) :
  Parcelable, Persistable, StableEquality {

  val children: List<WizardActionDependency> = children.toList()

  /** The type of dependency. */
  enum class Type(val value: Int) {
    /** A single action that must be completed. */
    ACTION(0),
    /** A group of dependencies, all of which must be satisfied. */
    ALL_OF(1),
    /** A group of dependencies, at least one of which must be satisfied. */
    ANY_OF(2);

    companion object {
      /** Returns the Type corresponding to the given value. */
      @JvmStatic
      fun fromValue(value: Int): Type {
        return values().find { it.value == value }
          ?: throw IllegalArgumentException("Unknown Type value: $value")
      }
    }
  }

  /** Interface to check if an action is completed. */
  fun interface CompletionChecker {
    /** Returns true if the action with the given ID is completed. */
    fun isActionCompleted(actionId: String): Boolean
  }

  /** Returns true if this dependency is satisfied according to the given completion checker. */
  fun isSatisfied(checker: CompletionChecker): Boolean {
    return when (type) {
      Type.ACTION -> actionId != null && checker.isActionCompleted(actionId)
      Type.ALL_OF -> children.all { it.isSatisfied(checker) }
      Type.ANY_OF -> children.any { it.isSatisfied(checker) }
    }
  }

  override fun describeContents(): Int = 0

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeInt(type.value)
    dest.writeString(actionId)
    dest.writeTypedList(children)
  }

  override fun toPersistableBundle(): PersistableBundle {
    val bundle = PersistableBundle()
    bundle.putInt(TYPE_KEY, type.value)
    if (actionId != null) {
      bundle.putString(ACTION_ID_KEY, actionId)
    }
    if (children.isNotEmpty()) {
      bundle.putPersistableList(CHILDREN_KEY, children)
    }
    return bundle
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as WizardActionDependency
    return type == that.type && actionId == that.actionId && children == that.children
  }

  override fun hashCode(): Int {
    return Objects.hash(type, actionId, children)
  }

  override fun toString(): String {
    return "WizardActionDependency{type=$type, actionId='$actionId', children=$children}"
  }

  companion object {
    private const val TYPE_KEY = "type"
    private const val ACTION_ID_KEY = "action_id"
    private const val CHILDREN_KEY = "children"

    /** Creates a dependency on a single action. */
    @JvmStatic
    fun action(actionId: String): WizardActionDependency {
      return WizardActionDependency(Type.ACTION, actionId, emptyList())
    }

    /** Creates an ALL_OF dependency group. */
    @JvmStatic
    fun allOf(children: List<WizardActionDependency>): WizardActionDependency {
      return WizardActionDependency(Type.ALL_OF, null, children)
    }

    /** Creates an ANY_OF dependency group. */
    @JvmStatic
    fun anyOf(children: List<WizardActionDependency>): WizardActionDependency {
      return WizardActionDependency(Type.ANY_OF, null, children)
    }

    @JvmField
    val CREATOR: Parcelable.Creator<WizardActionDependency> =
      object : Parcelable.Creator<WizardActionDependency> {
        override fun createFromParcel(inParcel: Parcel): WizardActionDependency {
          val type = Type.fromValue(inParcel.readInt())
          val actionId = inParcel.readString()
          val children = mutableListOf<WizardActionDependency>()
          inParcel.readTypedList(children, CREATOR)
          return WizardActionDependency(type, actionId, children)
        }

        override fun newArray(size: Int): Array<WizardActionDependency?> {
          return arrayOfNulls(size)
        }
      }
  }
}
