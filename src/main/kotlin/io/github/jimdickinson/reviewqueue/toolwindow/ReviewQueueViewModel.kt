package io.github.jimdickinson.reviewqueue.toolwindow

import kotlinx.coroutines.flow.StateFlow
import java.time.Clock

class ReviewQueueViewModel(
    private val source: ReviewRequestsSource,
    private val opener: PullRequestOpener,
    private val clock: Clock,
) {
    val state: StateFlow<ReviewQueueState>
        get() = TODO()

    suspend fun refresh(): Unit = TODO()

    fun open(row: ReviewRow): Unit = TODO()
}
