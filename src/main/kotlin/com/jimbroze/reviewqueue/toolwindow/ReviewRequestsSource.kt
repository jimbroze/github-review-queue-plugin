package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.queue.ReviewRequestsResult

fun interface ReviewRequestsSource {
    suspend fun fetch(): ReviewRequestsResult
}
