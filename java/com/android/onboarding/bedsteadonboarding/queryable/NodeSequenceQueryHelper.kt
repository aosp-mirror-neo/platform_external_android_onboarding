/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.onboarding.bedsteadonboarding.queryable

import android.os.Parcel
import android.os.Parcelable
import com.android.onboarding.nodes.OnboardingGraphNode
import com.android.queryable.Queryable
import com.android.queryable.util.ParcelableUtils

/**
 * Implementation of [NodeSequenceQuery].
 */
class NodeSequenceQueryHelper<E : Queryable> : NodeSequenceQuery<E> {

  @Transient private lateinit var query: E

  private var targetValue: Boolean? = null

  var nodeToCompareWith: OnboardingGraphNode? = null

  constructor(query: E) {
    this.query = query
  }

  constructor(parcel: Parcel) {
    targetValue = ParcelableUtils.readNullableBoolean(parcel)
    nodeToCompareWith = parcel.readParcelable(NodeSequenceQueryHelper::class.java.classLoader)!!
  }

  override fun isTrue(): E {
    check(targetValue == null) { "Cannot set multiple node sequence filters" }
    targetValue = true
    return query
  }

  override fun isFalse(): E {
    check(targetValue == null) { "Cannot set multiple node sequence filters" }
    targetValue = false
    return query
  }

  override fun isEmptyQuery(): Boolean {
    return targetValue == null || nodeToCompareWith == null
  }

  override fun matches(value: Boolean): Boolean {
    return targetValue == null || targetValue == value
  }

  override fun describeQuery(fieldName: String): String? {
    return if (targetValue == null) {
      null
    } else "$fieldName=$targetValue"
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(out: Parcel, flags: Int) {
    ParcelableUtils.writeNullableBoolean(out, targetValue)
  }

  companion object CREATOR : Parcelable.Creator<NodeSequenceQueryHelper<Queryable>> {
    override fun createFromParcel(parcel: Parcel): NodeSequenceQueryHelper<Queryable> {
      return NodeSequenceQueryHelper(parcel)
    }

    override fun newArray(size: Int): Array<NodeSequenceQueryHelper<Queryable>?> {
      return arrayOfNulls(size)
    }
  }
}
