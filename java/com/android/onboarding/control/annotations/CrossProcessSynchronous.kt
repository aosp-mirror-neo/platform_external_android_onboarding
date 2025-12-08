package com.android.onboarding.control.annotations

/**
 * Annotates a Task contract to indicate that it sometimes calls into a foreign component to execute
 * some synchronous code there. This task can't execute synchronously due to the asynchronous IPC
 * involved.
 *
 * We annotate this kind of tasks to avoid UI issues previously seen, e.g. b/445047938.
 */
annotation class CrossProcessSynchronous
