package io.github.jimdickinson.reviewqueue.queue

import java.time.Instant

sealed interface ReviewRequestsResult {
    data class Success(val requests: List<ReviewRequest>) : ReviewRequestsResult
    data object NotSignedIn : ReviewRequestsResult
    data object Unauthorized : ReviewRequestsResult
    data class RateLimited(val resetAt: Instant?) : ReviewRequestsResult
    data class Failed(val reason: String) : ReviewRequestsResult
}
