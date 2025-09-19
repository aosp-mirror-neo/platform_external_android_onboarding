package com.android.onboarding.tasks

import android.content.Context
import android.os.PersistableBundle
import com.android.onboarding.common.annotations.UniqueKey
import com.android.onboarding.metadata.OnboardingMetadata
import com.android.onboarding.utils.StableEquality
import com.android.onboarding.utils.persistable.PersistableDeserializer
import com.android.onboarding.utils.persistable.getPersistable
import com.android.onboarding.utils.persistable.putPersistable
import com.android.onboarding.utils.resources.BitmapResource
import com.android.onboarding.utils.resources.StringResource

/**
 * Basic descriptive metadata to be used by loading screens and notifications.
 *
 * @param header A string which can be used for the highest level description of this task. This may
 *   be used as the title of a notification or the header of a loading screen.
 * @param description More details about this task. This may be used as the body of a notification
 *   or for text below the header on a loading screen.
 * @param icon An icon which represents either this task overall, or the current status of the task
 * @param childDescriptiveTaskMetadata Optional list of child descriptive metadata that provide a
 *   more granular overview of this task. These metadata entries describe sub-tasks, items
 *   processed, or other relevant details. They will be displayed as sub-items in the loading screen
 *   to provide more detailed progress information.
 */
@UniqueKey("DescriptiveTaskMetadata")
data class DescriptiveTaskMetadata
@JvmOverloads
constructor(
  val header: StringResource? = null,
  val description: StringResource? = null,
  val icon: BitmapResource? = null,
  val childDescriptiveTaskMetadata: List<DescriptiveTaskMetadata> = emptyList(),
) : OnboardingMetadata, StableEquality {
  override fun toPersistableBundle(): PersistableBundle {
    val persistableBundle =
      PersistableBundle().apply {
        header?.let { putPersistable(KEY_HEADER, it) }
        description?.let { putPersistable(KEY_DESCRIPTION, it) }
        icon?.let { putPersistable(KEY_ICON, it) }
      }
    val numOfChildren = childDescriptiveTaskMetadata.size
    persistableBundle.putInt(KEY_NUM_OF_CHILDREN, numOfChildren)
    for (idx in 0..<numOfChildren) {
      persistableBundle.putPersistable("$KEY_CHILD_PREFIX$idx", childDescriptiveTaskMetadata[idx])
    }
    return persistableBundle
  }

  override fun toString(context: Context?): String {
    if (context == null) return super.toString(context)
    return "DescriptiveTaskMetadata{header=${header?.get(context)}, description=${description?.get(context)}, icon=$icon, childDescriptiveTaskMetadata=[${
      childDescriptiveTaskMetadata.joinToString(
        ", "
      ) { it.toString(context) }
    }]}"
  }

  companion object : PersistableDeserializer<DescriptiveTaskMetadata> {
    private const val KEY_ICON = "icon"
    private const val KEY_HEADER = "header"
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_NUM_OF_CHILDREN = "numOfChildren"
    private const val KEY_CHILD_PREFIX = "childMetadata_"

    override fun fromPersistableBundle(
      persistableBundle: PersistableBundle
    ): DescriptiveTaskMetadata {
      val listOfDescriptiveTaskMetadata = mutableListOf<DescriptiveTaskMetadata>()
      val numOfChildren = persistableBundle.getInt(KEY_NUM_OF_CHILDREN, 0)
      for (idx in 0..<numOfChildren) {
        listOfDescriptiveTaskMetadata.add(
          persistableBundle.getPersistable("$KEY_CHILD_PREFIX$idx", DescriptiveTaskMetadata)!!
        )
      }
      return DescriptiveTaskMetadata(
        header = persistableBundle.getPersistable(KEY_HEADER, StringResource),
        description = persistableBundle.getPersistable(KEY_DESCRIPTION, StringResource),
        icon = persistableBundle.getPersistable(KEY_ICON, BitmapResource),
        childDescriptiveTaskMetadata = listOfDescriptiveTaskMetadata,
      )
    }
  }
}
