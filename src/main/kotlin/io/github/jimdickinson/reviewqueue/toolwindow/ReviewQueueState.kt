package io.github.jimdickinson.reviewqueue.toolwindow

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef

sealed interface ReviewQueueState {
    data object Loading : ReviewQueueState
    data class Showing(val rows: List<ReviewRow>, val isRefreshing: Boolean) : ReviewQueueState
    data object NothingWaiting : ReviewQueueState
    data object NeedsSignIn : ReviewQueueState
    data object SignInExpired : ReviewQueueState
    data class RateLimited(val resetsIn: String?) : ReviewQueueState
    data class CouldNotLoad(val reason: String) : ReviewQueueState
}

val ReviewQueueState.isFetching: Boolean
    get() = this is ReviewQueueState.Loading || (this is ReviewQueueState.Showing && isRefreshing)

data class ReviewRow(
    val pullRequestLabel: String,
    val title: String,
    val authorLogin: String,
    val waitingFor: String,
    val requestedFrom: String,
    val isReReview: Boolean,
    val isDraft: Boolean,
    val pullRequest: PullRequestRef,
    val url: String,
)
