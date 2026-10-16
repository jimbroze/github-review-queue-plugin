package com.jimbroze.reviewqueue.queue

import com.jimbroze.reviewqueue.timeline.Reviewer
import com.jimbroze.reviewqueue.timeline.TimelineEvent.ReviewRequested
import com.jimbroze.reviewqueue.timeline.Viewer
import com.jimbroze.reviewqueue.timeline.pendingReviewFor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class QueueFilterTest {

    private val aTeam = RequestedReviewer.Team("platform")
    private val draft = request(number = 1, isDraft = true)
    private val teamRequest = request(number = 2, reviewer = aTeam)
    private val draftForTeam = request(number = 3, isDraft = true, reviewer = aTeam)
    private val readyForYou = request(number = 4)
    private val everything = listOf(draft, teamRequest, draftForTeam, readyForYou)

    @Test
    fun `by default every request is shown`() {
        assertEquals(everything, QueueFilter().applyTo(everything))
    }

    @Test
    fun `hiding drafts leaves only pull requests ready for review`() {
        assertEquals(listOf(teamRequest, readyForYou), QueueFilter(showDrafts = false).applyTo(everything))
    }

    @Test
    fun `excluding team requests leaves only requests made to you`() {
        assertEquals(listOf(draft, readyForYou), QueueFilter(includeTeamRequests = false).applyTo(everything))
    }

    @Test
    fun `both filters together leave only ready pull requests asked of you`() {
        val filter = QueueFilter(showDrafts = false, includeTeamRequests = false)

        assertEquals(listOf(readyForYou), filter.applyTo(everything))
    }

    @Test
    fun `filtering keeps the order it was given`() {
        val reversed = everything.reversed()

        assertEquals(listOf(readyForYou, draft), QueueFilter(includeTeamRequests = false).applyTo(reversed))
    }

    @Test
    fun `filtering out every request gives an empty queue`() {
        assertEquals(emptyList<ReviewRequest>(), QueueFilter(includeTeamRequests = false).applyTo(listOf(teamRequest)))
    }

    @Test
    fun `a pull request asked of both you and your team stays when team requests are excluded`() {
        val viewer = Viewer(login = "octocat", teamSlugs = setOf("platform"))
        val timeline = listOf(
            ReviewRequested(Instant.parse("2026-01-01T09:00:00Z"), Reviewer.Team("platform")),
            ReviewRequested(Instant.parse("2026-01-01T10:00:00Z"), Reviewer.User("octocat")),
        )
        val askedOfBoth = request(number = 5, reviewer = pendingReviewFor(viewer, timeline)!!.requestedReviewer)

        assertEquals(listOf(askedOfBoth), QueueFilter(includeTeamRequests = false).applyTo(listOf(askedOfBoth)))
    }

    private fun request(
        number: Long,
        isDraft: Boolean = false,
        reviewer: RequestedReviewer = RequestedReviewer.You,
    ) = ReviewRequest(
        pullRequest = PullRequestRef(RepositoryName("acme", "widgets"), number),
        title = "PR #$number",
        authorLogin = "alice",
        url = "https://github.com/acme/widgets/pull/$number",
        requestedAt = Instant.parse("2026-01-02T09:00:00Z"),
        requestedReviewer = reviewer,
        isReReview = false,
        isDraft = isDraft,
    )
}
