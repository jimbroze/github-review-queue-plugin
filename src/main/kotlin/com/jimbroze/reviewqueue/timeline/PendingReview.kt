package com.jimbroze.reviewqueue.timeline

import com.jimbroze.reviewqueue.queue.RequestedReviewer
import com.jimbroze.reviewqueue.timeline.TimelineEvent.ReviewRequestRemoved
import com.jimbroze.reviewqueue.timeline.TimelineEvent.ReviewRequested
import com.jimbroze.reviewqueue.timeline.TimelineEvent.ReviewSubmitted
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
fun pendingReviewFor(viewer: Viewer, timeline: List<TimelineEvent>): PendingReview? {
    val events = timeline.sortedBy { it.at }
    val request = mostUrgent(outstandingRequests(viewer, events)) ?: return null
    return PendingReview(
        requestedAt = request.since,
        requestedReviewer = request.reviewer,
        isReReview = viewerReviewedBefore(request.since, viewer, events),
    )
}

private data class OutstandingRequest(val reviewer: RequestedReviewer, val since: Instant)

private fun outstandingRequests(viewer: Viewer, chronologicalEvents: List<TimelineEvent>): Collection<OutstandingRequest> {
    val outstandingByReviewer = mutableMapOf<RequestedReviewer, OutstandingRequest>()
    for (event in chronologicalEvents) {
        when (event) {
            is ReviewRequested -> viewer.asRequestedReviewer(event.reviewer)?.let {
                outstandingByReviewer.putIfAbsent(it.caseInsensitiveKey(), OutstandingRequest(it, event.at))
            }
            is ReviewRequestRemoved -> viewer.asRequestedReviewer(event.reviewer)?.let {
                outstandingByReviewer.remove(it.caseInsensitiveKey())
            }
            is ReviewSubmitted -> if (viewer.isAuthorOf(event)) outstandingByReviewer.clear()
        }
    }
    return outstandingByReviewer.values
}

private fun mostUrgent(requests: Collection<OutstandingRequest>): OutstandingRequest? =
    requests.firstOrNull { it.reviewer == RequestedReviewer.You } ?: requests.minByOrNull { it.since }

private fun viewerReviewedBefore(time: Instant, viewer: Viewer, events: List<TimelineEvent>): Boolean =
    events.filterIsInstance<ReviewSubmitted>().any { viewer.isAuthorOf(it) && it.at.isBefore(time) }

private fun Viewer.asRequestedReviewer(reviewer: Reviewer): RequestedReviewer? = when (reviewer) {
    is Reviewer.User -> RequestedReviewer.You.takeIf { reviewer.login.equals(login, ignoreCase = true) }
    is Reviewer.Team -> RequestedReviewer.Team(reviewer.slug).takeIf { isMemberOf(reviewer) }
}

private fun Viewer.isMemberOf(team: Reviewer.Team): Boolean =
    teamSlugs.any { it.equals(team.slug, ignoreCase = true) }

private fun Viewer.isAuthorOf(review: ReviewSubmitted): Boolean =
    review.authorLogin.equals(login, ignoreCase = true)

private fun RequestedReviewer.caseInsensitiveKey(): RequestedReviewer = when (this) {
    RequestedReviewer.You -> this
    is RequestedReviewer.Team -> RequestedReviewer.Team(slug.lowercase())
}
