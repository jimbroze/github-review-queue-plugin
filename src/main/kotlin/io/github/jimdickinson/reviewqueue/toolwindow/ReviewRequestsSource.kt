package io.github.jimdickinson.reviewqueue.toolwindow

import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult

fun interface ReviewRequestsSource {
    suspend fun fetch(): ReviewRequestsResult
}
