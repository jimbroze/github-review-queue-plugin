package com.jimbroze.reviewqueue.toolwindow

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.Duration

/** Emits once every [interval], starting one [interval] from now, until the collector is cancelled. */
fun ticksEvery(interval: Duration): Flow<Unit> = flow {
    while (true) {
        delay(interval.toMillis())
        emit(Unit)
    }
}

/** Passes on this flow's values only while [condition] is true, collecting it afresh each time [condition] turns true. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.onlyWhile(condition: Flow<Boolean>): Flow<T> =
    condition.distinctUntilChanged().flatMapLatest { holds -> if (holds) this else emptyFlow() }
