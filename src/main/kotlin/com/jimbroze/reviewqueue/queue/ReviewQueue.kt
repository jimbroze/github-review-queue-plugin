package com.jimbroze.reviewqueue.queue

object ReviewQueue {
    private val reviewPriority: Comparator<ReviewRequest> =
        compareBy<ReviewRequest> { it.isDraft }
            .thenByDescending { it.isReReview }
            .thenBy { it.requestedAt }
            .thenByDescending { it.requestedReviewer == RequestedReviewer.You }

    fun order(requests: List<ReviewRequest>): List<ReviewRequest> = requests.sortedWith(reviewPriority)
}
