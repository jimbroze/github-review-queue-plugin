package com.jimbroze.reviewqueue.queue

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReviewQueueTest {

    private val aTeam = RequestedReviewer.Team("platform")
    private val earliest = Instant.parse("2026-01-01T09:00:00Z")
    private val earlier = Instant.parse("2026-01-02T09:00:00Z")
    private val later = Instant.parse("2026-01-03T09:00:00Z")

    @Test
    fun `an empty list of requests gives an empty queue`() {
        assertEquals(emptyList<ReviewRequest>(), ReviewQueue.order(emptyList()))
    }

    @Test
    fun `re-review requests come before first-time requests`() {
        val firstTime = request(number = 1, isReReview = false)
        val reReview = request(number = 2, isReReview = true)

        assertOrder(listOf(firstTime, reReview), expected = listOf(reReview, firstTime))
    }

    @Test
    fun `a re-review requested from a team comes before a first-time request to you`() {
        val firstTimeToYou = request(number = 1, isReReview = false, reviewer = RequestedReviewer.You)
        val reReviewToTeam = request(number = 2, isReReview = true, reviewer = aTeam)

        assertOrder(listOf(firstTimeToYou, reReviewToTeam), expected = listOf(reReviewToTeam, firstTimeToYou))
    }

    @Test
    fun `requests to you come before requests to your teams asked at the same time`() {
        val toTeam = request(number = 1, reviewer = aTeam)
        val toYou = request(number = 2, reviewer = RequestedReviewer.You)

        assertOrder(listOf(toTeam, toYou), expected = listOf(toYou, toTeam))
    }

    @Test
    fun `an older request to your team comes before a newer request to you`() {
        val newerToYou = request(number = 1, reviewer = RequestedReviewer.You, requestedAt = later)
        val olderToTeam = request(number = 2, reviewer = aTeam, requestedAt = earlier)

        assertOrder(listOf(newerToYou, olderToTeam), expected = listOf(olderToTeam, newerToYou))
    }

    @Test
    fun `the longest-waiting request comes first`() {
        val newest = request(number = 1, requestedAt = later)
        val oldest = request(number = 2, requestedAt = earliest)
        val middle = request(number = 3, requestedAt = earlier)

        assertOrder(listOf(newest, oldest, middle), expected = listOf(oldest, middle, newest))
    }

    @Test
    fun `requests that tie on every rule keep their input order`() {
        val first = request(number = 7)
        val second = request(number = 3)
        val third = request(number = 5)

        assertOrder(listOf(first, second, third), expected = listOf(first, second, third))
    }

    @Test
    fun `a mixed queue is ordered by re-review, then waiting time, then who was asked`() {
        val firstTimeToTeamOldest = request(number = 1, isReReview = false, reviewer = aTeam, requestedAt = earliest)
        val firstTimeToYouNewer = request(number = 2, isReReview = false, reviewer = RequestedReviewer.You, requestedAt = later)
        val firstTimeToYouOlder = request(number = 3, isReReview = false, reviewer = RequestedReviewer.You, requestedAt = earlier)
        val reReviewToTeamOldest = request(number = 4, isReReview = true, reviewer = aTeam, requestedAt = earliest)
        val reReviewToYouNewer = request(number = 5, isReReview = true, reviewer = RequestedReviewer.You, requestedAt = later)
        val reReviewToYouOlder = request(number = 6, isReReview = true, reviewer = RequestedReviewer.You, requestedAt = earlier)

        assertOrder(
            listOf(
                firstTimeToTeamOldest,
                firstTimeToYouNewer,
                firstTimeToYouOlder,
                reReviewToTeamOldest,
                reReviewToYouNewer,
                reReviewToYouOlder,
            ),
            expected = listOf(
                reReviewToTeamOldest,
                reReviewToYouOlder,
                reReviewToYouNewer,
                firstTimeToTeamOldest,
                firstTimeToYouOlder,
                firstTimeToYouNewer,
            ),
        )
    }

    @Test
    fun `pull requests ready for review come before drafts`() {
        val draft = request(number = 1, isDraft = true)
        val ready = request(number = 2, isDraft = false)

        assertOrder(listOf(draft, ready), expected = listOf(ready, draft))
    }

    @Test
    fun `a draft re-review waiting longest still comes after a newer first-time request ready for review`() {
        val draftReReviewOldest = request(number = 1, isDraft = true, isReReview = true, requestedAt = earliest)
        val readyFirstTimeNewer = request(number = 2, isDraft = false, isReReview = false, requestedAt = later)

        assertOrder(listOf(draftReReviewOldest, readyFirstTimeNewer), expected = listOf(readyFirstTimeNewer, draftReReviewOldest))
    }

    @Test
    fun `drafts are ordered among themselves by re-review, then waiting time, then who was asked`() {
        val toTeamOldest = request(number = 1, isDraft = true, reviewer = aTeam, requestedAt = earliest)
        val toYouNewer = request(number = 2, isDraft = true, reviewer = RequestedReviewer.You, requestedAt = later)
        val toYouSameTimeAsTeam = request(number = 3, isDraft = true, reviewer = RequestedReviewer.You, requestedAt = earliest)
        val reReviewNewest = request(number = 4, isDraft = true, isReReview = true, requestedAt = later)

        assertOrder(
            listOf(toTeamOldest, toYouNewer, toYouSameTimeAsTeam, reReviewNewest),
            expected = listOf(reReviewNewest, toYouSameTimeAsTeam, toTeamOldest, toYouNewer),
        )
    }

    @Test
    fun `with no sort rules the queue keeps the order it was given`() {
        val newerDraft = request(number = 1, isDraft = true, requestedAt = later)
        val olderReReview = request(number = 2, isReReview = true, requestedAt = earliest)

        assertOrder(listOf(newerDraft, olderReReview), SortOrder.of(emptyList()), expected = listOf(newerDraft, olderReReview))
    }

    @Test
    fun `sort rules apply from first to last`() {
        val olderToTeam = request(number = 1, reviewer = aTeam, requestedAt = earlier)
        val newerToYou = request(number = 2, reviewer = RequestedReviewer.You, requestedAt = later)
        val directThenWaiting = SortOrder.of(listOf(SortRule.DirectRequestsFirst, SortRule.LongestWaitingFirst))

        assertOrder(listOf(olderToTeam, newerToYou), directThenWaiting, expected = listOf(newerToYou, olderToTeam))
    }

    @Test
    fun `a rule after longest waiting only decides between requests asked at the same moment`() {
        val newerReReview = request(number = 1, isReReview = true, requestedAt = later)
        val olderFirstTime = request(number = 2, isReReview = false, requestedAt = earlier)
        val sameTimeReReview = request(number = 3, isReReview = true, requestedAt = earlier)
        val waitingThenReReview = SortOrder.of(listOf(SortRule.LongestWaitingFirst, SortRule.ReReviewsFirst))

        assertOrder(
            listOf(newerReReview, olderFirstTime, sameTimeReReview),
            waitingThenReReview,
            expected = listOf(sameTimeReReview, olderFirstTime, newerReReview),
        )
    }

    @Test
    fun `drafts mix with pull requests ready for review when drafts last is left out`() {
        val olderDraft = request(number = 1, isDraft = true, requestedAt = earliest)
        val newerReady = request(number = 2, isDraft = false, requestedAt = later)

        assertOrder(
            listOf(newerReady, olderDraft),
            SortOrder.of(listOf(SortRule.LongestWaitingFirst)),
            expected = listOf(olderDraft, newerReady),
        )
    }

    @Test
    fun `the default sort order gives the same queue as ordering without one`() {
        val requests = listOf(
            request(number = 1, isDraft = true, requestedAt = earliest),
            request(number = 2, reviewer = aTeam, requestedAt = earlier),
            request(number = 3, isReReview = true, requestedAt = later),
            request(number = 4, reviewer = RequestedReviewer.You, requestedAt = earlier),
        )

        assertEquals(ReviewQueue.order(requests), ReviewQueue.order(requests, SortOrder.Default))
    }

    private fun request(
        number: Long,
        isReReview: Boolean = false,
        reviewer: RequestedReviewer = RequestedReviewer.You,
        requestedAt: Instant = earlier,
        isDraft: Boolean = false,
    ) = ReviewRequest(
        pullRequest = PullRequestRef(RepositoryName("acme", "widgets"), number),
        title = "PR #$number",
        authorLogin = "alice",
        url = "https://github.com/acme/widgets/pull/$number",
        requestedAt = requestedAt,
        requestedReviewer = reviewer,
        isReReview = isReReview,
        isDraft = isDraft,
    )

    private fun assertOrder(input: List<ReviewRequest>, expected: List<ReviewRequest>) {
        assertEquals(
            expected.map { it.pullRequest.number },
            ReviewQueue.order(input).map { it.pullRequest.number },
        )
    }

    private fun assertOrder(input: List<ReviewRequest>, sortOrder: SortOrder, expected: List<ReviewRequest>) {
        assertEquals(
            expected.map { it.pullRequest.number },
            ReviewQueue.order(input, sortOrder).map { it.pullRequest.number },
        )
    }
}
