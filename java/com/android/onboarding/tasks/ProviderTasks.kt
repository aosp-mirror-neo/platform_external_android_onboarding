package com.android.onboarding.tasks

import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import com.android.onboarding.contracts.OnboardingActivityApiContract
import com.android.onboarding.nodes.ComponentRef
import com.android.onboarding.nodes.NodeRef
import com.android.onboarding.pending.PendingContract
import com.android.onboarding.utils.logging.OnboardingLog as Log
import com.android.onboarding.utils.persistable.PersistableIntent
import kotlinx.coroutines.delay

/** An [OnboardingTask] base for [ProviderContract] implementations. */
sealed class ProviderTask<I, O>(final override val contract: ProviderContract<I, O>) :
  OnboardingTask<I, O, ProviderContract<I, O>>()

class DiagnosisProviderTask(component: ComponentRef, private val binderIdentityHash: Int) :
  ProviderTask<DiagnosisProviderContract.Input, DiagnosisProviderContract.Output>(
    DiagnosisProviderContract(component)
  ) {

  override suspend fun runTask(
    context: Context,
    input: DiagnosisProviderContract.Input,
  ): DiagnosisProviderContract.Output {
    log.v("Collecting diagnostic information")
    input.delayFor?.let { duration ->
      log.v("Delaying for $duration before execution")
      delay(duration.toMillis())
    }

    return DiagnosisProviderContract.Output(ownerIdentityHash = binderIdentityHash)
  }

  private companion object {
    val log = Log.logger("DiagnosisProviderTask")
  }
}

/**
 * An implementation of [PendingContractProviderContract].
 *
 * @param component a [ComponentRef] of the component providing the data. This is a component
 *   hosting the task instance.
 * @param activities a list of [OnboardingActivityApiContracts][OnboardingActivityApiContract] which
 *   are exposed for IPC operations.
 * @param tasks a list of [OnboardingTaskContracts][OnboardingTaskContract] which are exposed for
 *   IPC operations.
 */
class PendingContractProviderTask(
  component: ComponentRef,
  activities: Set<OnboardingActivityApiContract<*, *>>,
  tasks: Set<OnboardingTaskContract<*, *>>,
) :
  ProviderTask<PendingContractProviderContract.Request, PendingContract.Erased<*>>(
    PendingContractProviderContract(component)
  ) {
  private val activities = activities.associateBy(NodeRef::asNodeRef)
  private val tasks = tasks.associateBy(NodeRef::asNodeRef)

  private fun <I> OnboardingActivityApiContract<I, *>.prepareFromRaw(
    context: Context,
    input: Intent,
  ) = prepare(context, extractArgument(input))

  private fun <I> OnboardingTaskContract<I, *>.prepareFromRaw(input: PersistableBundle) =
    prepare(inputDecoder.encode(input))

  override suspend fun runTask(
    context: Context,
    input: PendingContractProviderContract.Request,
  ): PendingContract.Erased<*> {
    Log.v(TAG, "Resolving PendingContract for ${input.identity()}")
    val ref = input.node.asNodeRef()
    val aContract = activities[ref]
    if (aContract != null) {
      return aContract
        .prepareFromRaw(context, PersistableIntent.fromPersistableBundle(input.input).toIntent())
        .erase()
        .also { Log.d(TAG, "Provided PendingContract.Activity for $ref") }
    }
    val tContract = tasks[ref]
    if (tContract != null) {
      return tContract.prepareFromRaw(input.input).erase().also {
        Log.d(TAG, "Provided PendingContract.Task for $ref")
      }
    }
    Log.e(TAG, "Failed to provide PendingContract for $ref")
    failNode("Node ${input.node.identity()} is not recognised by this provider")
  }

  private companion object {
    const val TAG = "PendingContractProvider"
  }
}

/**
 * An implementation of [ContractMetadataProviderContract].
 *
 * @param component a [ComponentRef] of the component providing the data. This is a component
 *   hosting the task instance.
 * @param nodes a list of all
 *   [OnboardingNodes][com.android.onboarding.contracts.annotations.OnboardingNode] which are
 *   exposed for IPC operations.
 */
class ContractMetadataProviderTask(component: ComponentRef, nodes: Collection<ContractMetadata>) :
  ProviderTask<Unit, Map<NodeRef.Stable, ContractMetadata>>(
    ContractMetadataProviderContract(component)
  ) {
  /**
   * @param component a [ComponentRef] of the component providing the data. This is a component
   *   hosting the task instance.
   * @param activities a list of [OnboardingActivityApiContracts][OnboardingActivityApiContract]
   *   which are exposed for IPC operations.
   * @param tasks a list of [OnboardingTaskContracts][OnboardingTaskContract] which are exposed for
   *   IPC operations.
   */
  constructor(
    component: ComponentRef,
    activities: Set<OnboardingActivityApiContract<*, *>>,
    tasks: Set<OnboardingTaskContract<*, *>>,
  ) : this(
    component = component,
    nodes = activities.map(ContractMetadata::invoke) + tasks.map(ContractMetadata::invoke),
  )

  private val nodes = nodes.distinct().associateBy(ContractMetadata::asNodeRef)

  override suspend fun runTask(
    context: Context,
    input: Unit,
  ): Map<NodeRef.Stable, ContractMetadata> {
    Log.v("ContractMetadataProvider", "Providing contract metadata index: size=${nodes.size}")
    return nodes
  }
}
