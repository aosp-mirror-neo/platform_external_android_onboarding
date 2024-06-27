package com.android.onboarding.bedsteadonboarding.queryable

import com.android.onboarding.nodes.OnboardingGraphNode
import com.android.queryable.Queryable
import com.android.queryable.queries.BooleanQuery
import com.android.queryable.queries.LongQuery
import com.android.queryable.queries.Query
import com.android.queryable.queries.StringQuery

/**
 * Query for onboarding graph nodes.
 */
interface NodeQuery<E: Queryable>: Query<OnboardingGraphNode> {
  /** Query a node based on its [id]. */
  fun id(): LongQuery<E>

  /** Query a node based on its [name]. */
  fun name(): StringQuery<E>

  /** Query a node based on its [component]. */
  fun component(): ComponentQuery<E>

  /**
   * Query for a started node. This query must have an [id], [name], or [component] filter
   * as well.
   */
  fun isStarted(): BooleanQuery<E>

  /**
   * Query for a finished node. This query must have an [id], [name], or [component] filter
   * as well.
   */
  fun isFinished(): BooleanQuery<E>

  /**
   * Query for a failed node. This query must have an [id], [name], or [component] filter
   * as well.
   */
  fun isFailed(): BooleanQuery<E>

  /** Query for all nodes that started before the specified [node]. */
  fun happenedBefore(node: OnboardingGraphNode): NodeSequenceQuery<E>

  /** Query for all nodes that started after the specified [node]. */
  fun happenedAfter(node: OnboardingGraphNode): NodeSequenceQuery<E>
}
