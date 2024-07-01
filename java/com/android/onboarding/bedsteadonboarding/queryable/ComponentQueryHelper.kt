package com.android.onboarding.bedsteadonboarding.queryable

import android.os.Parcel
import android.os.Parcelable
import com.android.onboarding.nodes.OnboardingGraphNodeData.Component
import com.android.queryable.Queryable
import com.android.queryable.queries.StringQueryHelper

/**
 * Implementation of [ComponentQuery].
 */
class ComponentQueryHelper<E: Queryable> : ComponentQuery<E> {

  @Transient private lateinit var query: E

  private val nameQueryHelper: StringQueryHelper<E>

  constructor(query: E) {
    this.query = query
    nameQueryHelper = StringQueryHelper(query)
  }

  constructor(parcel: Parcel) {
    nameQueryHelper = parcel.readParcelable(ComponentQueryHelper::class.java.classLoader) 
      as StringQueryHelper<E>? ?: throw IllegalStateException()
  }

  override fun name() = nameQueryHelper

  override fun matches(component: Component?): Boolean {
    return nameQueryHelper.matches(component?.name)
  }

  override fun describeQuery(fieldName: String?): String {
    return Queryable.joinQueryStrings(nameQueryHelper.describeQuery("$fieldName.name"))
  }

  override fun isEmptyQuery(): Boolean {
    return Queryable.isEmptyQuery(nameQueryHelper)
  }

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeParcelable(nameQueryHelper, flags)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<ComponentQueryHelper<Queryable>> {
    override fun createFromParcel(parcel: Parcel): ComponentQueryHelper<Queryable> {
      return ComponentQueryHelper(parcel)
    }

    override fun newArray(size: Int): Array<ComponentQueryHelper<Queryable>?> {
      return arrayOfNulls(size)
    }
  }
}
