package com.jimbroze.reviewqueue.toolwindow

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class TicksTest {
    private val ticks = MutableSharedFlow<Int>()
    private val watched = MutableStateFlow(false)

    @Test
    fun `ticks pass through only while the condition holds`() = runBlocking {
        val received = mutableListOf<Int>()
        val collecting = launch(start = CoroutineStart.UNDISPATCHED) { ticks.onlyWhile(watched).toList(received) }

        tick(1)
        watched.value = true
        yield()
        tick(2)
        watched.value = false
        yield()
        tick(3)
        watched.value = true
        yield()
        tick(4)
        collecting.cancel()

        assertEquals(listOf(2, 4), received)
    }

    private suspend fun tick(number: Int) {
        ticks.emit(number)
        yield()
    }
}
