package com.jimbroze.reviewqueue.toolwindow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean

/** Counts who is currently looking at the queue, so work can pause while nobody is. */
class QueueWatchers {
    private val countLock = Any()
    private var openWatches = 0
    private val watched = MutableStateFlow(false)

    val isWatched: StateFlow<Boolean> = watched.asStateFlow()

    /** Emits each time the queue goes from unwatched to watched. */
    val startedWatching: Flow<Unit> = isWatched.drop(1).filter { it }.map { }

    fun watch(): Watch {
        changeOpenWatches(+1)
        return Watch()
    }

    private fun changeOpenWatches(change: Int) = synchronized(countLock) {
        openWatches += change
        watched.value = openWatches > 0
    }

    inner class Watch : AutoCloseable {
        private val isOpen = AtomicBoolean(true)

        override fun close() {
            if (isOpen.compareAndSet(true, false)) changeOpenWatches(-1)
        }
    }
}
