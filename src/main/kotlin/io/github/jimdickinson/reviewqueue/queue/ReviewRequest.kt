package io.github.jimdickinson.reviewqueue.queue

import java.time.Instant

data class RepositoryName(val owner: String, val name: String)

data class PullRequestRef(val repository: RepositoryName, val number: Long)

sealed interface RequestedReviewer {
    data object You : RequestedReviewer
    data class Team(val slug: String) : RequestedReviewer
}

data class ReviewRequest(
    val pullRequest: PullRequestRef,
    val title: String,
    val url: String,
    val requestedAt: Instant,
    val requestedReviewer: RequestedReviewer,
    val isReReview: Boolean,
)
