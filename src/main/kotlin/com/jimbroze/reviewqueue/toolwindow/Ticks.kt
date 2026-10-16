package com.jimbroze.reviewqueue.toolwindow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration

/** Emits once every [interval], starting one [interval] from now, until the collector is cancelled. */
fun ticksEvery(interval: Duration): Flow<Unit> = flow {
    while (true) {
        delay(interval.toMillis())
        emit(Unit)
    }
}
