package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.queue.PullRequestRef
import com.jimbroze.reviewqueue.queue.RepositoryName
import com.jimbroze.reviewqueue.queue.RequestedReviewer
import com.jimbroze.reviewqueue.queue.ReviewRequest
import com.jimbroze.reviewqueue.queue.ReviewRequestsResult
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.CouldNotLoad
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.Loading
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.NeedsSignIn
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.NothingWaiting
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.RateLimited
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.Showing
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.SignInExpired
import com.jimbroze.reviewqueue.worktime.ReviewTiming
import com.jimbroze.reviewqueue.worktime.WorkHours
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class ReviewQueueViewModelTest {

    private val now = Instant.parse("2026-10-15T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val timing = ReviewTiming(WorkHours.officeHours(ZoneOffset.UTC))
    private val openedPullRequests = mutableListOf<Pair<PullRequestRef, String>>()
    private val recordingOpener = PullRequestOpener { pullRequest, url -> openedPullRequests += pullRequest to url }

    @Test
    fun `before the first refresh the queue is loading`() {
        val viewModel = viewModelReturning(ReviewRequestsResult.Success(emptyList()))

        assertEquals(Loading, viewModel.state.value)
    }

    @Test
    fun `the first load shows loading`() = runBlocking {
        val stillFetching = CompletableDeferred<ReviewRequestsResult>()
        val viewModel = ReviewQueueViewModel({ stillFetching.await() }, recordingOpener, clock, timing)

        assertEquals(Loading, stateWhileFetching(viewModel, stillFetching))
    }

    @Test
    fun `refreshing keeps the current rows visible while it fetches`() = runBlocking {
        val loaded = CompletableDeferred<ReviewRequestsResult>(ReviewRequestsResult.Success(listOf(request(number = 1))))
        val stillFetching = CompletableDeferred<ReviewRequestsResult>()
        val responses = ArrayDeque(listOf(loaded, stillFetching))
        val viewModel = ReviewQueueViewModel({ responses.removeFirst().await() }, recordingOpener, clock, timing)
        viewModel.refresh()

        val state = stateWhileFetching(viewModel, stillFetching) as Showing

        assertEquals(listOf(1L), state.rows.map { it.pullRequest.number })
        assertTrue(state.isRefreshing)
    }

    @Test
    fun `a loaded list is not marked as refreshing`() {
        val state = stateAfterRefresh(ReviewRequestsResult.Success(listOf(request()))) as Showing

        assertFalse(state.isRefreshing)
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
    fun `a row shows how much working time the review has been waiting`() {
        val row = refreshedRows(request(requestedAt = Instant.parse("2026-10-14T15:00:00Z"))).single()

        assertEquals("5 hours", row.waitingFor)
    }

    @Test
    fun `time outside working hours is not counted in the wait`() {
        val row = refreshedRows(request(requestedAt = Instant.parse("2026-10-14T20:00:00Z"))).single()

        assertEquals("3 hours", row.waitingFor)
    }

    @Test
    fun `a wait of a working day or more is shown in work days`() {
        val row = refreshedRows(request(requestedAt = Instant.parse("2026-10-12T12:00:00Z"))).single()

        assertEquals("3 work days", row.waitingFor)
    }

    @Test
    fun `a row shows who opened the pull request`() {
        val row = refreshedRows(request(authorLogin = "alice")).single()

        assertEquals("alice", row.authorLogin)
    }

    @Test
    fun `a draft row is marked as a draft`() {
        val row = refreshedRows(request(isDraft = true)).single()

        assertTrue(row.isDraft)
    }

    @Test
    fun `a row ready for review is not marked as a draft`() {
        val row = refreshedRows(request(isDraft = false)).single()

        assertFalse(row.isDraft)
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
        assertEquals(NothingWaiting(), stateAfterRefresh(ReviewRequestsResult.Success(emptyList())))
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

    @Test
    fun `a fetch that throws unexpectedly shows that the queue could not load`() = runBlocking {
        val viewModel = ReviewQueueViewModel({ throw IllegalStateException("connection reset") }, recordingOpener, clock, timing)
        viewModel.refresh()

        assertEquals(CouldNotLoad("connection reset"), viewModel.state.value)
    }

    @Test
    fun `a cancelled fetch is not shown as a failure`() {
        val viewModel = ReviewQueueViewModel({ throw CancellationException("tool window closed") }, recordingOpener, clock, timing)

        assertThrows(CancellationException::class.java) { runBlocking { viewModel.refresh() } }
        assertFalse(viewModel.state.value is CouldNotLoad)
    }

    private suspend fun stateWhileFetching(
        viewModel: ReviewQueueViewModel,
        pendingResponse: CompletableDeferred<ReviewRequestsResult>,
    ): ReviewQueueState = coroutineScope {
        val refresh = launch { viewModel.refresh() }
        yield()
        val state = viewModel.state.value
        pendingResponse.complete(ReviewRequestsResult.Success(emptyList()))
        refresh.join()
        state
    }

    @Test
    fun `a warning that came with the list is shown with the rows`() {
        val state = stateAfterRefresh(ReviewRequestsResult.Success(listOf(request()), warnings = listOf("Some teams are hidden")))

        assertEquals(listOf("Some teams are hidden"), (state as Showing).warnings)
    }

    @Test
    fun `a warning that came with an empty list is shown with the nothing-waiting message`() {
        val state = stateAfterRefresh(ReviewRequestsResult.Success(emptyList(), warnings = listOf("Some teams are hidden")))

        assertEquals(NothingWaiting(warnings = listOf("Some teams are hidden")), state)
    }

    @Test
    fun `a list without warnings shows none`() {
        val state = stateAfterRefresh(ReviewRequestsResult.Success(listOf(request())))

        assertEquals(emptyList<String>(), state.warnings)
    }

    private fun viewModelReturning(vararg results: ReviewRequestsResult): ReviewQueueViewModel {
        val remaining = ArrayDeque(results.toList())
        return ReviewQueueViewModel({ remaining.removeFirst() }, recordingOpener, clock, timing)
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
        authorLogin: String = "alice",
        isDraft: Boolean = false,
    ) = ReviewRequest(
        pullRequest = PullRequestRef(RepositoryName(owner, repository), number),
        title = title,
        authorLogin = authorLogin,
        url = "https://github.com/$owner/$repository/pull/$number",
        requestedAt = requestedAt,
        requestedReviewer = reviewer,
        isReReview = isReReview,
        isDraft = isDraft,
    )
}
