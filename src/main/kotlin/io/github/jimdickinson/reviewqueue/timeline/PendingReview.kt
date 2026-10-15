package io.github.jimdickinson.reviewqueue.timeline

import io.github.jimdickinson.reviewqueue.queue.RequestedReviewer
import java.time.Instant

data class Viewer(val login: String, val teamSlugs: Set<String>)

data class PendingReview(
    val requestedAt: Instant,
    val requestedReviewer: RequestedReviewer,
    val isReReview: Boolean,
)

/**
 * Returns the review [viewer] currently owes on a pull request with the given [timeline],
 * or null when no review is requested from them. [timeline] may be in any order.
 */
fun pendingReviewFor(viewer: Viewer, timeline: List<TimelineEvent>): PendingReview? = TODO()
