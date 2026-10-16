package com.jimbroze.reviewqueue.queue

object ReviewQueue {
    fun order(requests: List<ReviewRequest>, sortOrder: SortOrder = SortOrder.Default): List<ReviewRequest> =
        requests.sortedWith(sortOrder.comparator)
}
