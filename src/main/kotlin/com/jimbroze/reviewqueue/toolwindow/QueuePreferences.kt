package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.queue.QueueFilter
import com.jimbroze.reviewqueue.queue.SortOrder
import com.jimbroze.reviewqueue.worktime.ReviewTiming

/** The user's choices for how the review queue is shown. */
data class QueuePreferences(
    val timing: ReviewTiming,
    val sortOrder: SortOrder = SortOrder.Default,
    val filter: QueueFilter = QueueFilter(),
)
