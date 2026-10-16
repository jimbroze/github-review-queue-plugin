package com.jimbroze.reviewqueue.toolwindow

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueWatchersTest {
    private val watchers = QueueWatchers()

    @Test
    fun `nobody watches the queue at first`() {
        assertFalse(watchers.isWatched.value)
    }

    @Test
    fun `the queue is watched while a watch is open`() {
        watchers.watch()

        assertTrue(watchers.isWatched.value)
    }

    @Test
    fun `the queue stays watched until every watch closes`() {
        val first = watchers.watch()
        val second = watchers.watch()

        first.close()
        assertTrue(watchers.isWatched.value)

        second.close()
        assertFalse(watchers.isWatched.value)
    }

    @Test
    fun `closing a watch twice only ends that watch`() {
        val first = watchers.watch()
        watchers.watch()

        first.close()
        first.close()

        assertTrue(watchers.isWatched.value)
    }

    @Test
    fun `each time someone starts watching an unwatched queue it is announced`() = runBlocking {
        val announcements = mutableListOf<Unit>()
        val listening = launch(start = CoroutineStart.UNDISPATCHED) { watchers.startedWatching.toList(announcements) }

        val first = watchers.watch()
        yield()
        watchers.watch().close()
        yield()
        first.close()
        yield()
        watchers.watch()
        yield()
        listening.cancel()

        assertEquals(2, announcements.size)
    }
}
