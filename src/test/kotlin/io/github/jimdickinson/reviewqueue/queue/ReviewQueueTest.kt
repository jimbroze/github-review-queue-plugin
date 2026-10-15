package io.github.jimdickinson.reviewqueue.queue

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
    fun `requests to you come before requests to your teams`() {
        val toTeam = request(number = 1, reviewer = aTeam)
        val toYou = request(number = 2, reviewer = RequestedReviewer.You)

        assertOrder(listOf(toTeam, toYou), expected = listOf(toYou, toTeam))
    }

    @Test
    fun `a newer request to you comes before an older request to your team`() {
        val olderToTeam = request(number = 1, reviewer = aTeam, requestedAt = earlier)
        val newerToYou = request(number = 2, reviewer = RequestedReviewer.You, requestedAt = later)

        assertOrder(listOf(olderToTeam, newerToYou), expected = listOf(newerToYou, olderToTeam))
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
    fun `a mixed queue is ordered by re-review, then who was asked, then waiting time`() {
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
                reReviewToYouOlder,
                reReviewToYouNewer,
                reReviewToTeamOldest,
                firstTimeToYouOlder,
                firstTimeToYouNewer,
                firstTimeToTeamOldest,
            ),
        )
    }

    private fun request(
        number: Long,
        isReReview: Boolean = false,
        reviewer: RequestedReviewer = RequestedReviewer.You,
        requestedAt: Instant = earlier,
    ) = ReviewRequest(
        pullRequest = PullRequestRef(RepositoryName("acme", "widgets"), number),
        title = "PR #$number",
        url = "https://github.com/acme/widgets/pull/$number",
        requestedAt = requestedAt,
        requestedReviewer = reviewer,
        isReReview = isReReview,
    )

    private fun assertOrder(input: List<ReviewRequest>, expected: List<ReviewRequest>) {
        assertEquals(
            expected.map { it.pullRequest.number },
            ReviewQueue.order(input).map { it.pullRequest.number },
        )
    }
}
