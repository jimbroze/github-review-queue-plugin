package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.CouldNotLoad
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.Loading
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.NothingWaiting
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.Showing
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewQueueStateTest {

    @Test
    fun `the first load is fetching`() {
        assertTrue(Loading.isFetching)
    }

    @Test
    fun `a list being refreshed is fetching`() {
        assertTrue(Showing(emptyList(), isRefreshing = true).isFetching)
    }

    @Test
    fun `a loaded list is not fetching`() {
        assertFalse(Showing(emptyList(), isRefreshing = false).isFetching)
    }

    @Test
    fun `a settled state without rows is not fetching`() {
        assertFalse(NothingWaiting().isFetching)
        assertFalse(CouldNotLoad("timed out").isFetching)
    }
}
