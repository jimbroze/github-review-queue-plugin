package com.jimbroze.reviewqueue.queue

data class QueueFilter(
    val showDrafts: Boolean = true,
    val includeTeamRequests: Boolean = true,
) {
    fun applyTo(requests: List<ReviewRequest>): List<ReviewRequest> = requests.filter(::isShown)

    private fun isShown(request: ReviewRequest): Boolean =
        (showDrafts || !request.isDraft) &&
            (includeTeamRequests || request.requestedReviewer == RequestedReviewer.You)
}
