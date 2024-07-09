package com.android.onboarding.bedsteadonboarding.queryable

import com.android.onboarding.nodes.OnboardingGraphNodeData.Component
import com.android.queryable.Queryable
import com.android.queryable.queries.Query
import com.android.queryable.queries.StringQuery

/**
 * Query for onboarding graph node components.
 */
interface ComponentQuery<E: Queryable>: Query<Component> {

  /** Query a node component based on its [name]. */
  fun name(): StringQuery<E>

}
