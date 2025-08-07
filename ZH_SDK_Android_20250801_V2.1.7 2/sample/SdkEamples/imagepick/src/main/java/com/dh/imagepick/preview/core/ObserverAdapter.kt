package com.dh.imagepick.preview.core

import androidx.lifecycle.Lifecycle
import com.dh.imagepick.preview.utils.bindLifecycle
import io.reactivex.observers.DisposableObserver

open class ObserverAdapter<T>(lifecycle: Lifecycle?) : DisposableObserver<T>() {
    init {
        bindLifecycle(lifecycle)
    }

    override fun onNext(t: T & Any) {

    }

    override fun onError(e: Throwable) {
    }

    override fun onComplete() {
    }
}