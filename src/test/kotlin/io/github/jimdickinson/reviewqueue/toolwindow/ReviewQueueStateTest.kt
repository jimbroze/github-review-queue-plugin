package io.github.jimdickinson.reviewqueue.toolwindow

import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.CouldNotLoad
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.Loading
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.NothingWaiting
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.Showing
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
