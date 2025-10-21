package com.android.onboarding.tasks.service

/** @see IObserverHandle */
inline fun IObserverHandle(crossinline onClose: () -> Unit) =
  object : IObserverHandle.Stub() {
    override fun version() = 1

    override fun close() = onClose()
  }

/** @see IObserver */
inline fun IObserver(
  crossinline onClose: () -> Unit,
  crossinline onError: (IPersistableBundleStream) -> Unit,
  crossinline onChange: (IPersistableBundleStream) -> Unit,
) =
  object : IObserver.Stub() {
    override fun version() = 1

    override fun onChange(state: IPersistableBundleStream) = onChange(state)

    override fun onError(error: IPersistableBundleStream) = onError(error)

    override fun onClose() = onClose()
  }

/** @see IUnitCallback */
inline fun IUnitCallback(
  crossinline onError: (IPersistableBundleStream) -> Unit,
  crossinline onSuccess: () -> Unit,
) =
  object : IUnitCallback.Stub() {
    override fun version() = 1

    override fun onSuccess() = onSuccess()

    override fun onError(error: IPersistableBundleStream) = onError(error)
  }

/** @see IResultCallback */
inline fun IResultCallback(
  crossinline onError: (IPersistableBundleStream) -> Unit,
  crossinline onSuccess: (IPersistableBundleStream) -> Unit,
) =
  object : IResultCallback.Stub() {
    override fun version() = 1

    override fun onSuccess(state: IPersistableBundleStream) = onSuccess(state)

    override fun onError(error: IPersistableBundleStream) = onError(error)
  }
