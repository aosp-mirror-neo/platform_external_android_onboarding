package com.android.onboarding.bedsteadonboarding.data

/** In-memory representation of the per test configuration */
@JvmInline value class TestConfigData(val testNodes: List<NodeData>)

/** Represents a unique identifier of a node's contract which is allowed to execute in a test. */
@JvmInline value class NodeData(val allowedContractIdentifier: String)
