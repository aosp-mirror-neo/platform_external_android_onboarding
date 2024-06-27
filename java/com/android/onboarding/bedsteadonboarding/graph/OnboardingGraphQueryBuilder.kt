package com.android.onboarding.bedsteadonboarding.graph

import com.android.onboarding.bedsteadonboarding.queryable.NodeQueryHelper
import com.android.onboarding.nodes.OnboardingGraphNode
import com.android.queryable.Queryable

/**
 * Builder for progressively building queries for [OnboardingGraph.Node].
 */
class OnboardingGraphQueryBuilder(provider: OnboardingGraphProvider) : Queryable {

  private val provider: OnboardingGraphProvider = provider

  private val node = NodeQueryHelper<OnboardingGraphQueryBuilder>(this)

  /**
   * Query for an [OnboardingGraph.Node].
   */
  fun whereNode(): NodeQueryHelper<OnboardingGraphQueryBuilder> = node

  /**
   * Get the matching [OnboardingGraph.Node].
   *
   * returns null if there is no matching [OnboardingGraph.Node].
   */
  fun get(): OnboardingGraphNode? {
    for (node in provider.graph.nodes.values) {
      if (!matches(node)) {
        continue
      }

      return node
    }

    return null
  }

  private fun matches(value: OnboardingGraphNode): Boolean {
    return node.matches(value)
  }

  override fun describeQuery(fieldName: String?): String {
    return Queryable.joinQueryStrings(
      node.describeQuery(fieldName)
    )
  }

  override fun isEmptyQuery(): Boolean {
    return Queryable.isEmptyQuery(node)
  }
}
