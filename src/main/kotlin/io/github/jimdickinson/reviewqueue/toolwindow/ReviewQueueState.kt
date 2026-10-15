package io.github.jimdickinson.reviewqueue.toolwindow

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef

sealed interface ReviewQueueState {
    data object Loading : ReviewQueueState
    data class Showing(val rows: List<ReviewRow>) : ReviewQueueState
    data object NothingWaiting : ReviewQueueState
    data object NeedsSignIn : ReviewQueueState
    data object SignInExpired : ReviewQueueState
    data class RateLimited(val resetsIn: String?) : ReviewQueueState
    data class CouldNotLoad(val reason: String) : ReviewQueueState
}

data class ReviewRow(
    val pullRequestLabel: String,
    val title: String,
    val waitingFor: String,
    val requestedFrom: String,
    val isReReview: Boolean,
    val pullRequest: PullRequestRef,
    val url: String,
)
