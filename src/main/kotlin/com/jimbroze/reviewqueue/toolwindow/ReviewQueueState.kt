package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.queue.PullRequestRef
import com.jimbroze.reviewqueue.worktime.Urgency

sealed interface ReviewQueueState {
    val warnings: List<String> get() = emptyList()

    data object Loading : ReviewQueueState
    data class Showing(
        val rows: List<ReviewRow>,
        val isRefreshing: Boolean,
        override val warnings: List<String> = emptyList(),
    ) : ReviewQueueState
    data class NothingWaiting(override val warnings: List<String> = emptyList()) : ReviewQueueState
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
    val urgency: Urgency,
    val requestedFrom: String,
    val isReReview: Boolean,
    val isDraft: Boolean,
    val pullRequest: PullRequestRef,
    val url: String,
)
