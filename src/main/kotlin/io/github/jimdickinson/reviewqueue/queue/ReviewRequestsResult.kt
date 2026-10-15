package io.github.jimdickinson.reviewqueue.queue

import java.time.Instant

sealed interface ReviewRequestsResult {
    data class Success(val requests: List<ReviewRequest>) : ReviewRequestsResult

    sealed interface Failure : ReviewRequestsResult
    data object NotSignedIn : Failure
    data object Unauthorized : Failure
    data class RateLimited(val resetAt: Instant?) : Failure
    data class Failed(val reason: String) : Failure
}
