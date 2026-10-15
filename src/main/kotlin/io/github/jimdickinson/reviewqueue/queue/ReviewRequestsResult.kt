package io.github.jimdickinson.reviewqueue.queue

sealed interface ReviewRequestsResult {
    data class Success(val requests: List<ReviewRequest>) : ReviewRequestsResult
}
