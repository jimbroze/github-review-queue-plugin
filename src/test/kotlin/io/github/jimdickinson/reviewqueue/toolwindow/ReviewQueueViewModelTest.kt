package io.github.jimdickinson.reviewqueue.toolwindow

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import io.github.jimdickinson.reviewqueue.queue.RequestedReviewer
import io.github.jimdickinson.reviewqueue.queue.ReviewRequest
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.CouldNotLoad
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.Loading
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.NeedsSignIn
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.NothingWaiting
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.RateLimited
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.Showing
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueState.SignInExpired
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class ReviewQueueViewModelTest {

    private val now = Instant.parse("2026-10-15T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val openedPullRequests = mutableListOf<Pair<PullRequestRef, String>>()
    private val recordingOpener = PullRequestOpener { pullRequest, url -> openedPullRequests += pullRequest to url }

    @Test
    fun `before the first refresh the queue is loading`() {
        val viewModel = viewModelReturning(ReviewRequestsResult.Success(emptyList()))

        assertEquals(Loading, viewModel.state.value)
    }

    @Test
    fun `the queue is loading while the requests are being fetched`() = runBlocking {
        val loaded = CompletableDeferred<ReviewRequestsResult>(ReviewRequestsResult.Success(emptyList()))
        val stillFetching = CompletableDeferred<ReviewRequestsResult>()
        val responses = ArrayDeque(listOf(loaded, stillFetching))
        val viewModel = ReviewQueueViewModel({ responses.removeFirst().await() }, recordingOpener, clock)
        viewModel.refresh()

        val secondRefresh = launch { viewModel.refresh() }
        yield()
        val stateWhileFetching = viewModel.state.value
        stillFetching.complete(ReviewRequestsResult.Success(emptyList()))
        secondRefresh.join()

        assertEquals(Loading, stateWhileFetching)
    }

    @Test
    fun `rows are shown in review queue order`() {
        val newer = request(number = 1, requestedAt = now.minus(Duration.ofHours(1)))
        val reReview = request(number = 2, requestedAt = now.minus(Duration.ofMinutes(5)), isReReview = true)
        val older = request(number = 3, requestedAt = now.minus(Duration.ofDays(2)))

        val rows = refreshedRows(newer, reReview, older)

        assertEquals(listOf(2L, 3L, 1L), rows.map { it.pullRequest.number })
    }

    @Test
    fun `a row shows the repository and pull request number`() {
        val row = refreshedRows(request(owner = "acme", repository = "widgets", number = 42)).single()

        assertEquals("acme/widgets #42", row.pullRequestLabel)
    }

    @Test
    fun `a row shows the pull request title`() {
        val row = refreshedRows(request(title = "Fix the flaky login test")).single()

        assertEquals("Fix the flaky login test", row.title)
    }

    @Test
    fun `a row shows how long the review has been waiting`() {
        val row = refreshedRows(request(requestedAt = now.minus(Duration.ofDays(3)))).single()

        assertEquals("3 days", row.waitingFor)
    }

    @Test
    fun `a row requested from you says it was requested from you`() {
        val row = refreshedRows(request(reviewer = RequestedReviewer.You)).single()

        assertEquals("you", row.requestedFrom)
    }

    @Test
    fun `a row requested from a team names the team`() {
        val row = refreshedRows(request(reviewer = RequestedReviewer.Team("acme/platform"))).single()

        assertEquals("@acme/platform", row.requestedFrom)
    }

    @Test
    fun `a re-review row is marked as a re-review`() {
        val row = refreshedRows(request(isReReview = true)).single()

        assertTrue(row.isReReview)
    }

    @Test
    fun `a first-time review row is not marked as a re-review`() {
        val row = refreshedRows(request(isReReview = false)).single()

        assertFalse(row.isReReview)
    }

    @Test
    fun `no requests shows that nothing is waiting on your review`() {
        assertEquals(NothingWaiting, stateAfterRefresh(ReviewRequestsResult.Success(emptyList())))
    }

    @Test
    fun `refreshing fetches the list again`() = runBlocking {
        val viewModel = viewModelReturning(
            ReviewRequestsResult.Success(listOf(request(number = 1))),
            ReviewRequestsResult.Success(listOf(request(number = 2))),
        )
        viewModel.refresh()
        viewModel.refresh()

        assertEquals(listOf(2L), (viewModel.state.value as Showing).rows.map { it.pullRequest.number })
    }

    @Test
    fun `opening a row opens that pull request`() = runBlocking {
        val viewModel = viewModelReturning(ReviewRequestsResult.Success(listOf(request(number = 7))))
        viewModel.refresh()

        viewModel.open((viewModel.state.value as Showing).rows.single())

        val expected = PullRequestRef(RepositoryName("acme", "widgets"), 7) to "https://github.com/acme/widgets/pull/7"
        assertEquals(listOf(expected), openedPullRequests)
    }

    @Test
    fun `a missing GitHub account asks you to sign in`() {
        assertEquals(NeedsSignIn, stateAfterRefresh(ReviewRequestsResult.NotSignedIn))
    }

    @Test
    fun `a rejected token asks you to sign in again`() {
        assertEquals(SignInExpired, stateAfterRefresh(ReviewRequestsResult.Unauthorized))
    }

    @Test
    fun `a rate limit says how long until it resets`() {
        val state = stateAfterRefresh(ReviewRequestsResult.RateLimited(resetAt = now.plus(Duration.ofMinutes(12))))

        assertEquals(RateLimited(resetsIn = "12 minutes"), state)
    }

    @Test
    fun `a rate limit with an unknown reset time does not say when it resets`() {
        assertEquals(RateLimited(resetsIn = null), stateAfterRefresh(ReviewRequestsResult.RateLimited(resetAt = null)))
    }

    @Test
    fun `a failed fetch shows the reason`() {
        val state = stateAfterRefresh(ReviewRequestsResult.Failed("GitHub returned HTTP 502"))

        assertEquals(CouldNotLoad("GitHub returned HTTP 502"), state)
    }

    @Test
    fun `refreshing after a failure shows the list once it loads`() = runBlocking {
        val viewModel = viewModelReturning(
            ReviewRequestsResult.Failed("timed out"),
            ReviewRequestsResult.Success(listOf(request(number = 1))),
        )
        viewModel.refresh()
        viewModel.refresh()

        assertTrue(viewModel.state.value is Showing)
    }

    private fun viewModelReturning(vararg results: ReviewRequestsResult): ReviewQueueViewModel {
        val remaining = ArrayDeque(results.toList())
        return ReviewQueueViewModel({ remaining.removeFirst() }, recordingOpener, clock)
    }

    private fun stateAfterRefresh(result: ReviewRequestsResult): ReviewQueueState = runBlocking {
        val viewModel = viewModelReturning(result)
        viewModel.refresh()
        viewModel.state.value
    }

    private fun refreshedRows(vararg requests: ReviewRequest): List<ReviewRow> =
        (stateAfterRefresh(ReviewRequestsResult.Success(requests.toList())) as Showing).rows

    private fun request(
        owner: String = "acme",
        repository: String = "widgets",
        number: Long = 1,
        title: String = "PR #$number",
        requestedAt: Instant = now.minus(Duration.ofHours(1)),
        reviewer: RequestedReviewer = RequestedReviewer.You,
        isReReview: Boolean = false,
    ) = ReviewRequest(
        pullRequest = PullRequestRef(RepositoryName(owner, repository), number),
        title = title,
        url = "https://github.com/$owner/$repository/pull/$number",
        requestedAt = requestedAt,
        requestedReviewer = reviewer,
        isReReview = isReReview,
    )
}
