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
import com.android.onboarding.bedsteadonboarding.contractutils.ContractUtils
import com.android.onboarding.nodes.OnboardingEvent
import com.android.onboarding.nodes.OnboardingGraphNode
import com.android.onboarding.nodes.OnboardingGraphLog
import com.android.queryable.Queryable
import com.android.queryable.queries.BooleanQueryHelper
import com.android.queryable.queries.LongQueryHelper
import com.android.queryable.queries.StringQueryHelper
import java.time.Instant
import kotlin.collections.Collection

/**
 * Implementation of [NodeQuery].
 */
class NodeQueryHelper<E : Queryable> : NodeQuery<E> {
  @Transient private lateinit var query: E

  private val idQueryHelper: LongQueryHelper<E>
  private val componentQueryHelper: ComponentQueryHelper<E>
  private val nameQueryHelper: StringQueryHelper<E>
  private val startedQueryHelper: BooleanQueryHelper<E>
  private val finishedQueryHelper: BooleanQueryHelper<E>
  private val failedQueryHelper: BooleanQueryHelper<E>
  private val happenedBeforeQueryHelpers: HashMap<OnboardingGraphNode, NodeSequenceQueryHelper<E>>
  private val happenedAfterQueryHelpers: HashMap<OnboardingGraphNode, NodeSequenceQueryHelper<E>>

  constructor(query: E) {
    this.query = query
    idQueryHelper = LongQueryHelper(query)
    componentQueryHelper = ComponentQueryHelper(query)
    nameQueryHelper = StringQueryHelper(query)
    startedQueryHelper = BooleanQueryHelper(query)
    finishedQueryHelper = BooleanQueryHelper(query)
    failedQueryHelper = BooleanQueryHelper(query)
    happenedBeforeQueryHelpers = HashMap()
    happenedAfterQueryHelpers = HashMap()
  }

  constructor(parcel: Parcel) {
    idQueryHelper = parcel.readParcelable(NodeQueryHelper::class.java.classLoader) 
      as LongQueryHelper<E>? ?: throw IllegalStateException()
    componentQueryHelper = parcel.readParcelable(NodeQueryHelper::class.java.classLoader) 
      as ComponentQueryHelper<E>? ?: throw IllegalStateException()
    nameQueryHelper = parcel.readParcelable(NodeQueryHelper::class.java.classLoader) 
      as StringQueryHelper<E>? ?: throw IllegalStateException()
    startedQueryHelper = parcel.readParcelable(NodeQueryHelper::class.java.classLoader) 
      as BooleanQueryHelper<E>? ?: throw IllegalStateException()
    finishedQueryHelper = parcel.readParcelable(NodeQueryHelper::class.java.classLoader) 
      as BooleanQueryHelper<E>? ?: throw IllegalStateException()
    failedQueryHelper = parcel.readParcelable(NodeQueryHelper::class.java.classLoader) 
      as BooleanQueryHelper<E>? ?: throw IllegalStateException()
    happenedBeforeQueryHelpers = parcel.readParcelable(NodeQueryHelper::class.java.classLoader) 
      as HashMap<OnboardingGraphNode, NodeSequenceQueryHelper<E>>? ?: throw IllegalStateException()
    happenedAfterQueryHelpers = parcel.readParcelable(NodeQueryHelper::class.java.classLoader) 
      as HashMap<OnboardingGraphNode, NodeSequenceQueryHelper<E>>? ?: throw IllegalStateException()
  }

  override fun id() = idQueryHelper

  override fun name() = nameQueryHelper

  override fun component() = componentQueryHelper

  override fun isStarted() = startedQueryHelper

  override fun isFinished() = finishedQueryHelper

  override fun isFailed() = failedQueryHelper

  override fun happenedBefore(node: OnboardingGraphNode): NodeSequenceQuery<E> {
    if (happenedBeforeQueryHelpers.containsKey(node)) {
      return happenedBeforeQueryHelpers[node]!!
    }

    val nodeSequenceQuery = NodeSequenceQueryHelper(query)
    nodeSequenceQuery.nodeToCompareWith = node
    happenedBeforeQueryHelpers[node] = nodeSequenceQuery

    return nodeSequenceQuery
  }

  override fun happenedAfter(node: OnboardingGraphNode): NodeSequenceQuery<E> {
    if (happenedAfterQueryHelpers.containsKey(node)) {
      return happenedAfterQueryHelpers[node]!!
    }

    val nodeSequenceQuery = NodeSequenceQueryHelper(query)
    nodeSequenceQuery.nodeToCompareWith = node
    happenedAfterQueryHelpers[node] = nodeSequenceQuery

    return nodeSequenceQuery
  }

  override fun describeQuery(fieldName: String?): String {
    val queryStrings = ArrayList<String>()

    queryStrings.add(idQueryHelper.describeQuery("$fieldName.id"))
    queryStrings.add(componentQueryHelper.describeQuery("$fieldName.component"))
    queryStrings.add(nameQueryHelper.describeQuery("$fieldName.name"))
    queryStrings.add(startedQueryHelper.describeQuery("$fieldName.started"))
    queryStrings.add(finishedQueryHelper.describeQuery("$fieldName.finished"))
    queryStrings.add(failedQueryHelper.describeQuery("$fieldName.failed"))

    for (query in happenedBeforeQueryHelpers.values) {
      queryStrings.add(query.describeQuery("$fieldName.happenedBefore")!!)
    }

    for (query in happenedAfterQueryHelpers.values) {
      queryStrings.add(query.describeQuery("$fieldName.happenedAfter")!!)
    }

    return Queryable.joinQueryStrings(queryStrings)
  }

  override fun isEmptyQuery(): Boolean {
    val isEmptyQuery = Queryable.isEmptyQuery(idQueryHelper) &&
      Queryable.isEmptyQuery(componentQueryHelper) &&
      Queryable.isEmptyQuery(nameQueryHelper) &&
      Queryable.isEmptyQuery(startedQueryHelper) &&
      Queryable.isEmptyQuery(finishedQueryHelper) &&
      Queryable.isEmptyQuery(failedQueryHelper)

    if (!isEmptyQuery) {
      return false
    }

    for (query in happenedBeforeQueryHelpers.values) {
      if (!Queryable.isEmptyQuery(query)) {
        return false
      }
    }

    for (query in happenedAfterQueryHelpers.values) {
      if (!Queryable.isEmptyQuery(query)) {
        return false
      }
    }

    return true
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(out: Parcel, flags: Int) {
    out.writeParcelable(idQueryHelper, flags)
    out.writeParcelable(componentQueryHelper, flags)
    out.writeParcelable(nameQueryHelper, flags)
    out.writeParcelable(startedQueryHelper, flags)
    out.writeParcelable(finishedQueryHelper, flags)
    out.writeParcelable(failedQueryHelper, flags)
    out.writeMap(happenedBeforeQueryHelpers)
    out.writeMap(happenedAfterQueryHelpers)
  }

  override fun matches(node: OnboardingGraphNode): Boolean {
    val matches = idQueryHelper.matches(node.id) &&
      componentQueryHelper.matches(node.component) &&
      nameQueryHelper.matches(node.name) &&
      startedQueryHelper.matches(isNodeStarted(node)) &&
      finishedQueryHelper.matches(isNodeFinished(node)) &&
      failedQueryHelper.matches(isNodeFailed(node))

    if (!matches) {
      return false
    }

    for (query in happenedBeforeQueryHelpers.values) {
      if (!query.matches(isHappenedBefore(node))) {
        return false
      }
    }

    for (query in happenedAfterQueryHelpers.values) {
      if (!query.matches(isHappenedAfter(node))) {
        return false
      }
    }

    return true
  }

  private fun isNodeStarted(node: OnboardingGraphNode): Boolean {
    return node.start < Instant.now()
  }

  private fun isNodeFinished(node: OnboardingGraphNode): Boolean {
    return node.end < Instant.now() &&
      !isAnotherNodeAttemptedToBeExecutedForResult(
        node.events, ContractUtils.getContractIdentifier(node.component!!.name, node.name)) &&
      node.outgoingEdgesOfValidNodes.isEmpty()
  }

  private fun isNodeFailed(node: OnboardingGraphNode): Boolean {
    return node.isFailed
  }

  private fun isHappenedBefore(node: OnboardingGraphNode): Boolean {
    if (Queryable.isEmptyQuery(happenedBeforeQueryHelpers[node])) {
      return true
    }
    return node.start.isBefore(happenedBeforeQueryHelpers.getValue(node).nodeToCompareWith!!.start)
  }

  private fun isHappenedAfter(node: OnboardingGraphNode): Boolean {
    if (Queryable.isEmptyQuery(happenedAfterQueryHelpers[node])) {
      return true
    }
    return node.start.isAfter(happenedAfterQueryHelpers.getValue(node).nodeToCompareWith!!.start)
  }

  private fun isAnotherNodeAttemptedToBeExecutedForResult(
    events: Collection<OnboardingGraphLog.OnboardingEventDelegate>,
    contractIdentifierOfNode: String
  ): Boolean {
    return events.any {
      val event = it.source
      (event is OnboardingEvent.ActivityNodeStartExecuteSynchronously) &&
        (ContractUtils.getContractIdentifier(event.nodeComponent, event.nodeName) !=
          contractIdentifierOfNode)
    }
  }

  companion object CREATOR : Parcelable.Creator<NodeQueryHelper<Queryable>> {
    override fun createFromParcel(parcel: Parcel): NodeQueryHelper<Queryable> {
      return NodeQueryHelper(parcel)
    }

    override fun newArray(size: Int): Array<NodeQueryHelper<Queryable>?> {
      return arrayOfNulls(size)
    }
  }
}
