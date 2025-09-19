package com.android.onboarding.contracts.annotations

/**
 * Annotation used to indicate that a contract is a pure function.
 *
 * This means that the implementation of this contract must not result in any state changes and must
 * not depend on any state. It is purely a mapping of input to output.
 *
 * Currently this has no enforcement. It is important that all implementations of contracts
 * annotated with this obey this contract or it will result in unpredictable issues in Onboarding.
 * If in doubt, this annotation should *not* be applied.
 *
 * When applied, this annotation allows for optimisations such as precalculating future effects
 * which results in a smoother experience for users.
 */

// contracts
annotation class PureContract
