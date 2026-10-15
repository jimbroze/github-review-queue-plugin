package io.github.jimdickinson.reviewqueue.queue

object ReviewQueue {
    private val reviewPriority: Comparator<ReviewRequest> =
        compareByDescending<ReviewRequest> { it.isReReview }
            .thenBy { it.requestedAt }
            .thenByDescending { it.requestedReviewer == RequestedReviewer.You }

    fun order(requests: List<ReviewRequest>): List<ReviewRequest> = requests.sortedWith(reviewPriority)
}
