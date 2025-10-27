package com.android.onboarding.tasks

import com.android.onboarding.tasks.utils.TaskResult

/** Class containing [callback] which will be invoked when result has been collected. */
interface ResultCollector<OutputT, MetadataT> {

  /** Called by task when there is an intermediate update [metadata] */
  fun onIntermediateUpdate(metadata: MetadataT)

  /** Called by task when complete with [taskResult] and [metadata] */
  fun onResultCollected(metadata: MetadataT?, taskResult: TaskResult<OutputT>)
}
