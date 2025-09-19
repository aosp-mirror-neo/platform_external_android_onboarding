package com.android.onboarding.nodes

import android.os.PersistableBundle
import com.android.onboarding.utils.persistable.PersistableDelegate

/** [PersistableDelegate] for use with [ComponentRef]. */
object PersistableComponentRef : PersistableDelegate<ComponentRef> {
  override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
    ComponentRef.fromPersistableBundle(persistableBundle)

  override fun toPersistableBundle(value: ComponentRef) =
    value.asComponentRef().toPersistableBundle()
}

/** [PersistableDelegate] for use with [NodeRef]. */
object PersistableNodeRef : PersistableDelegate<NodeRef> {
  override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
    NodeRef.fromPersistableBundle(persistableBundle)

  override fun toPersistableBundle(value: NodeRef): PersistableBundle =
    value.asNodeRef().toPersistableBundle()
}

/** [PersistableDelegate] for use with [UniqueNodeRef]. */
object PersistableUniqueNodeRef : PersistableDelegate<UniqueNodeRef> {
  override fun fromPersistableBundle(persistableBundle: PersistableBundle) =
    UniqueNodeRef.fromPersistableBundle(persistableBundle)

  override fun toPersistableBundle(value: UniqueNodeRef): PersistableBundle =
    value.asUniqueNodeRef().toPersistableBundle()
}
