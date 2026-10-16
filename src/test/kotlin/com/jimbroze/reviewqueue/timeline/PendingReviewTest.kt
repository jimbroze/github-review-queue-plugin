package com.jimbroze.reviewqueue.timeline

import com.jimbroze.reviewqueue.queue.RequestedReviewer
import com.jimbroze.reviewqueue.timeline.TimelineEvent.ReviewRequestRemoved
import com.jimbroze.reviewqueue.timeline.TimelineEvent.ReviewRequested
import com.jimbroze.reviewqueue.timeline.TimelineEvent.ReviewSubmitted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PendingReviewTest {

    private val viewer = Viewer(login = "octocat", teamSlugs = setOf("platform", "security"))
    private val you = Reviewer.User("octocat")
    private val yourTeam = Reviewer.Team("platform")
    private val someoneElse = Reviewer.User("hubot")
    private val teamYouAreNotIn = Reviewer.Team("design")

    @Test
    fun `a direct request to you is pending from the time it was made`() {
        val pending = pendingReviewFor(viewer, listOf(requested(1, you)))

        assertEquals(PendingReview(time(1), RequestedReviewer.You, isReReview = false), pending)
    }

    @Test
    fun `a request to a team you belong to is pending as that team`() {
        val pending = pendingReviewFor(viewer, listOf(requested(1, yourTeam)))

        assertEquals(PendingReview(time(1), RequestedReviewer.Team("platform"), isReReview = false), pending)
    }

    @Test
    fun `an empty timeline has no pending review`() {
        assertNull(pendingReviewFor(viewer, emptyList()))
    }

    @Test
    fun `requests to other people are ignored`() {
        assertNull(pendingReviewFor(viewer, listOf(requested(1, someoneElse))))
    }

    @Test
    fun `requests to teams you are not in are ignored`() {
        assertNull(pendingReviewFor(viewer, listOf(requested(1, teamYouAreNotIn))))
    }

    @Test
    fun `a request removed after it was made is no longer pending`() {
        val timeline = listOf(requested(1, you), removed(2, you))

        assertNull(pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `a request is no longer pending once you submit a review`() {
        val timeline = listOf(requested(1, you), reviewed(2))

        assertNull(pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `a team request is no longer pending once you submit a review`() {
        val timeline = listOf(requested(1, yourTeam), reviewed(2))

        assertNull(pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `a review by someone else does not clear your request`() {
        val timeline = listOf(requested(1, you), reviewed(2, by = someoneElse.login))

        assertEquals(time(1), pendingReviewFor(viewer, timeline)?.requestedAt)
    }

    @Test
    fun `removing a request for someone else does not clear yours`() {
        val timeline = listOf(requested(1, you), requested(2, someoneElse), removed(3, someoneElse))

        assertEquals(time(1), pendingReviewFor(viewer, timeline)?.requestedAt)
    }

    @Test
    fun `removing the team request leaves your direct request pending`() {
        val timeline = listOf(requested(1, you), requested(2, yourTeam), removed(3, yourTeam))

        assertEquals(PendingReview(time(1), RequestedReviewer.You, isReReview = false), pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `removing your direct request leaves the team request pending`() {
        val timeline = listOf(requested(1, yourTeam), requested(2, you), removed(3, you))

        assertEquals(
            PendingReview(time(1), RequestedReviewer.Team("platform"), isReReview = false),
            pendingReviewFor(viewer, timeline),
        )
    }

    @Test
    fun `a request made again after removal is pending from the new request`() {
        val timeline = listOf(requested(1, you), removed(2, you), requested(3, you))

        assertEquals(PendingReview(time(3), RequestedReviewer.You, isReReview = false), pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `being requested again without reviewing in between keeps the wait from the first request`() {
        val timeline = listOf(requested(1, you), requested(5, you))

        assertEquals(time(1), pendingReviewFor(viewer, timeline)?.requestedAt)
    }

    @Test
    fun `a direct request outranks a team request even when the team request is older`() {
        val timeline = listOf(requested(1, yourTeam), requested(5, you))

        assertEquals(PendingReview(time(5), RequestedReviewer.You, isReReview = false), pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `a direct request outranks a team request made after it`() {
        val timeline = listOf(requested(1, you), requested(5, yourTeam))

        assertEquals(PendingReview(time(1), RequestedReviewer.You, isReReview = false), pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `when several of your teams are requested the earliest outstanding team request is used`() {
        val timeline = listOf(requested(5, yourTeam), requested(2, Reviewer.Team("security")))

        assertEquals(
            PendingReview(time(2), RequestedReviewer.Team("security"), isReReview = false),
            pendingReviewFor(viewer, timeline),
        )
    }

    @Test
    fun `being requested again after you reviewed is a re-review from the new request`() {
        val timeline = listOf(requested(1, you), reviewed(2), requested(3, you))

        assertEquals(PendingReview(time(3), RequestedReviewer.You, isReReview = true), pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `a team request after you reviewed is a re-review`() {
        val timeline = listOf(requested(1, you), reviewed(2), requested(3, yourTeam))

        assertEquals(
            PendingReview(time(3), RequestedReviewer.Team("platform"), isReReview = true),
            pendingReviewFor(viewer, timeline),
        )
    }

    @Test
    fun `a review you submitted without being requested makes a later request a re-review`() {
        val timeline = listOf(reviewed(1), requested(2, you))

        assertEquals(true, pendingReviewFor(viewer, timeline)?.isReReview)
    }

    @Test
    fun `a review by someone else before the request does not make it a re-review`() {
        val timeline = listOf(reviewed(1, by = someoneElse.login), requested(2, you))

        assertEquals(false, pendingReviewFor(viewer, timeline)?.isReReview)
    }

    @Test
    fun `being requested twice after reviewing is a re-review from the first of those requests`() {
        val timeline = listOf(requested(1, you), reviewed(2), requested(3, you), requested(4, you))

        assertEquals(PendingReview(time(3), RequestedReviewer.You, isReReview = true), pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `events are read in time order regardless of list order`() {
        val timeline = listOf(requested(3, you), reviewed(2), requested(1, you))

        assertEquals(PendingReview(time(3), RequestedReviewer.You, isReReview = true), pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `a removal listed before its request still clears it`() {
        val timeline = listOf(removed(2, you), requested(1, you))

        assertNull(pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `your login matches requests regardless of case`() {
        val timeline = listOf(requested(1, Reviewer.User("OctoCat")))

        assertEquals(RequestedReviewer.You, pendingReviewFor(viewer, timeline)?.requestedReviewer)
    }

    @Test
    fun `your login matches your reviews regardless of case`() {
        val timeline = listOf(requested(1, you), reviewed(2, by = "OCTOCAT"))

        assertNull(pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `your login matches removals regardless of case`() {
        val timeline = listOf(requested(1, you), removed(2, Reviewer.User("OctoCat")))

        assertNull(pendingReviewFor(viewer, timeline))
    }

    @Test
    fun `team slugs match regardless of case and keep the slug as requested`() {
        val timeline = listOf(requested(1, Reviewer.Team("Platform")))

        assertEquals(RequestedReviewer.Team("Platform"), pendingReviewFor(viewer, timeline)?.requestedReviewer)
    }

    private fun time(minute: Int): Instant = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(minute * 60L)

    private fun requested(minute: Int, reviewer: Reviewer) = ReviewRequested(time(minute), reviewer)

    private fun removed(minute: Int, reviewer: Reviewer) = ReviewRequestRemoved(time(minute), reviewer)

    private fun reviewed(minute: Int, by: String = viewer.login) = ReviewSubmitted(time(minute), by)
}
