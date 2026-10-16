package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.queue.PullRequestRef
import com.jimbroze.reviewqueue.queue.QueueFilter
import com.jimbroze.reviewqueue.queue.RepositoryName
import com.jimbroze.reviewqueue.queue.RequestedReviewer
import com.jimbroze.reviewqueue.queue.ReviewRequest
import com.jimbroze.reviewqueue.queue.ReviewRequestsResult
import com.jimbroze.reviewqueue.queue.SortOrder
import com.jimbroze.reviewqueue.queue.SortRule
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.CouldNotLoad
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.Loading
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.NeedsSignIn
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.NothingWaiting
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.RateLimited
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.Showing
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueState.SignInExpired
import com.jimbroze.reviewqueue.worktime.ReviewTiming
import com.jimbroze.reviewqueue.worktime.Urgency
import com.jimbroze.reviewqueue.worktime.WorkHours
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class ReviewQueueViewModelTest {

    private val now = Instant.parse("2026-10-15T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val preferences = QueuePreferences(timing = ReviewTiming.defaultsIn(ZoneOffset.UTC))
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
        val viewModel = ReviewQueueViewModel({ stillFetching.await() }, recordingOpener, clock, preferences)

        assertEquals(Loading, stateWhileFetching(viewModel, stillFetching))
    }

    @Test
    fun `refreshing keeps the current rows visible while it fetches`() = runBlocking {
        val loaded = CompletableDeferred<ReviewRequestsResult>(ReviewRequestsResult.Success(listOf(request(number = 1))))
        val stillFetching = CompletableDeferred<ReviewRequestsResult>()
        val responses = ArrayDeque(listOf(loaded, stillFetching))
        val viewModel = ReviewQueueViewModel({ responses.removeFirst().await() }, recordingOpener, clock, preferences)
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
        val viewModel = ReviewQueueViewModel({ throw IllegalStateException("connection reset") }, recordingOpener, clock, preferences)
        viewModel.refresh()

        assertEquals(CouldNotLoad("connection reset"), viewModel.state.value)
    }

    @Test
    fun `a cancelled fetch is not shown as a failure`() {
        val viewModel = ReviewQueueViewModel({ throw CancellationException("tool window closed") }, recordingOpener, clock, preferences)

        assertThrows(CancellationException::class.java) { runBlocking { viewModel.refresh() } }
        assertFalse(viewModel.state.value is CouldNotLoad)
    }

    @Test
    fun `a request waiting a short time is on time`() {
        val row = refreshedRows(request(requestedAt = now.minus(Duration.ofHours(1)))).single()

        assertEquals(Urgency.OnTime, row.urgency)
    }

    @Test
    fun `a request waiting past the nearly-overdue time is nearly overdue`() {
        val row = refreshedRows(request(requestedAt = now.minus(Duration.ofHours(3)))).single()

        assertEquals(Urgency.NearlyOverdue, row.urgency)
    }

    @Test
    fun `a request waiting past the overdue time is overdue`() {
        val row = refreshedRows(request(requestedAt = Instant.parse("2026-10-14T16:00:00Z"))).single()

        assertEquals(Urgency.Overdue, row.urgency)
    }

    @Test
    fun `a draft is never overdue`() {
        val row = refreshedRows(request(requestedAt = Instant.parse("2026-10-12T12:00:00Z"), isDraft = true)).single()

        assertEquals(Urgency.OnTime, row.urgency)
    }

    @Test
    fun `waiting times keep updating without fetching again`() = runBlocking {
        val movableClock = MovableClock(now)
        var fetches = 0
        val viewModel = ReviewQueueViewModel(
            { fetches++; ReviewRequestsResult.Success(listOf(request(requestedAt = now.minus(Duration.ofHours(1))))) },
            recordingOpener,
            movableClock,
            preferences,
        )
        viewModel.refresh()
        val ticks = MutableSharedFlow<Unit>()
        val updating = launch(start = CoroutineStart.UNDISPATCHED) { viewModel.keepWaitingTimesCurrent(ticks) }

        movableClock.now = now.plus(Duration.ofHours(3))
        ticks.tick()
        updating.cancel()

        val row = (viewModel.state.value as Showing).rows.single()
        assertEquals("4 hours", row.waitingFor)
        assertEquals(Urgency.Overdue, row.urgency)
        assertEquals(1, fetches)
    }

    @Test
    fun `updating waiting times keeps the warnings`() = runBlocking {
        val movableClock = MovableClock(now)
        val viewModel = ReviewQueueViewModel(
            { ReviewRequestsResult.Success(listOf(request(requestedAt = now.minus(Duration.ofHours(1)))), warnings = listOf("Some teams are hidden")) },
            recordingOpener,
            movableClock,
            preferences,
        )
        viewModel.refresh()
        val ticks = MutableSharedFlow<Unit>()
        val updating = launch(start = CoroutineStart.UNDISPATCHED) { viewModel.keepWaitingTimesCurrent(ticks) }

        movableClock.now = now.plus(Duration.ofHours(1))
        ticks.tick()
        updating.cancel()

        val updated = viewModel.state.value as Showing
        assertEquals("2 hours", updated.rows.single().waitingFor)
        assertEquals(listOf("Some teams are hidden"), updated.warnings)
    }

    @Test
    fun `updating waiting times leaves other messages alone`() = runBlocking {
        val viewModel = viewModelReturning(ReviewRequestsResult.NotSignedIn)
        viewModel.refresh()
        val ticks = MutableSharedFlow<Unit>()
        val updating = launch(start = CoroutineStart.UNDISPATCHED) { viewModel.keepWaitingTimesCurrent(ticks) }

        ticks.tick()
        updating.cancel()

        assertEquals(NeedsSignIn, viewModel.state.value)
    }

    private suspend fun MutableSharedFlow<Unit>.tick() {
        emit(Unit)
        yield()
    }

    @Test
    fun `new overdue times recolour the rows without fetching again`() = runBlocking {
        var fetches = 0
        val viewModel = ReviewQueueViewModel(
            { fetches++; ReviewRequestsResult.Success(listOf(request(requestedAt = now.minus(Duration.ofHours(2))))) },
            recordingOpener,
            clock,
            preferences,
        )
        viewModel.refresh()

        viewModel.usePreferences(preferencesWithTiming(nearlyOverdueAfter = Duration.ofHours(1), overdueAfter = Duration.ofHours(2)))

        assertEquals(Urgency.Overdue, (viewModel.state.value as Showing).rows.single().urgency)
        assertEquals(1, fetches)
    }

    @Test
    fun `new work hours change the waiting time shown`() = runBlocking {
        val viewModel = viewModelReturning(ReviewRequestsResult.Success(listOf(request(requestedAt = now.minus(Duration.ofHours(2))))))
        viewModel.refresh()

        viewModel.usePreferences(QueuePreferences(timing = ReviewTiming(
            WorkHours(LocalTime.of(11, 0), LocalTime.of(18, 0), DayOfWeek.entries.toSet(), ZoneOffset.UTC),
            nearlyOverdueAfter = Duration.ofHours(3),
            overdueAfter = Duration.ofHours(4),
        )))

        assertEquals("1 hour", (viewModel.state.value as Showing).rows.single().waitingFor)
    }

    @Test
    fun `new preferences keep the rows visible while a refresh is running`() = runBlocking {
        val loaded = CompletableDeferred<ReviewRequestsResult>(ReviewRequestsResult.Success(listOf(request(number = 1))))
        val stillFetching = CompletableDeferred<ReviewRequestsResult>()
        val responses = ArrayDeque(listOf(loaded, stillFetching))
        val viewModel = ReviewQueueViewModel({ responses.removeFirst().await() }, recordingOpener, clock, preferences)
        viewModel.refresh()

        val state = coroutineScope {
            val refresh = launch { viewModel.refresh() }
            yield()
            viewModel.usePreferences(preferencesWithTiming(nearlyOverdueAfter = Duration.ofHours(5), overdueAfter = Duration.ofHours(6)))
            val state = viewModel.state.value
            stillFetching.complete(ReviewRequestsResult.Success(emptyList()))
            refresh.join()
            state
        }

        assertTrue((state as Showing).isRefreshing)
    }

    @Test
    fun `new preferences before the first load leave the queue loading`() {
        val viewModel = viewModelReturning(ReviewRequestsResult.Success(emptyList()))

        viewModel.usePreferences(preferencesWithTiming(nearlyOverdueAfter = Duration.ofHours(1), overdueAfter = Duration.ofHours(2)))

        assertEquals(Loading, viewModel.state.value)
    }

    @Test
    fun `new preferences leave other messages alone`() = runBlocking {
        val viewModel = viewModelReturning(ReviewRequestsResult.Failed("timed out"))
        viewModel.refresh()

        viewModel.usePreferences(preferencesWithTiming(nearlyOverdueAfter = Duration.ofHours(1), overdueAfter = Duration.ofHours(2)))

        assertEquals(CouldNotLoad("timed out"), viewModel.state.value)
    }

    @Test
    fun `rows follow the preferred sort order`() = runBlocking {
        val older = request(number = 1, requestedAt = now.minus(Duration.ofHours(2)))
        val reReview = request(number = 2, requestedAt = now.minus(Duration.ofHours(1)), isReReview = true)
        val viewModel = viewModelReturning(ReviewRequestsResult.Success(listOf(reReview, older)))
        viewModel.usePreferences(preferences.copy(sortOrder = SortOrder.of(listOf(SortRule.LongestWaitingFirst))))
        viewModel.refresh()

        assertEquals(listOf(1L, 2L), (viewModel.state.value as Showing).rows.map { it.pullRequest.number })
    }

    @Test
    fun `a new sort order reorders the rows without fetching again`() = runBlocking {
        var fetches = 0
        val draft = request(number = 1, requestedAt = now.minus(Duration.ofHours(2)), isDraft = true)
        val ready = request(number = 2, requestedAt = now.minus(Duration.ofHours(1)))
        val viewModel = ReviewQueueViewModel(
            { fetches++; ReviewRequestsResult.Success(listOf(draft, ready)) },
            recordingOpener,
            clock,
            preferences,
        )
        viewModel.refresh()

        viewModel.usePreferences(preferences.copy(sortOrder = SortOrder.of(listOf(SortRule.LongestWaitingFirst))))

        assertEquals(listOf(1L, 2L), (viewModel.state.value as Showing).rows.map { it.pullRequest.number })
        assertEquals(1, fetches)
    }

    @Test
    fun `drafts can be hidden`() {
        val rows = refreshedRowsWith(
            preferences.copy(filter = QueueFilter(showDrafts = false)),
            request(number = 1, isDraft = true),
            request(number = 2),
        )

        assertEquals(listOf(2L), rows.map { it.pullRequest.number })
    }

    @Test
    fun `team requests can be hidden`() {
        val rows = refreshedRowsWith(
            preferences.copy(filter = QueueFilter(includeTeamRequests = false)),
            request(number = 1, reviewer = RequestedReviewer.Team("acme/platform")),
            request(number = 2, reviewer = RequestedReviewer.You),
        )

        assertEquals(listOf(2L), rows.map { it.pullRequest.number })
    }

    @Test
    fun `when every request is hidden nothing is waiting, keeping the warnings`() = runBlocking {
        val viewModel = viewModelReturning(
            ReviewRequestsResult.Success(listOf(request(isDraft = true)), warnings = listOf("Some teams are hidden")),
        )
        viewModel.usePreferences(preferences.copy(filter = QueueFilter(showDrafts = false)))
        viewModel.refresh()

        assertEquals(NothingWaiting(warnings = listOf("Some teams are hidden")), viewModel.state.value)
    }

    @Test
    fun `showing hidden requests again brings their rows back without fetching`() = runBlocking {
        var fetches = 0
        val viewModel = ReviewQueueViewModel(
            { fetches++; ReviewRequestsResult.Success(listOf(request(number = 1, isDraft = true))) },
            recordingOpener,
            clock,
            preferences.copy(filter = QueueFilter(showDrafts = false)),
        )
        viewModel.refresh()

        viewModel.usePreferences(preferences.copy(filter = QueueFilter(showDrafts = true)))

        assertEquals(listOf(1L), (viewModel.state.value as Showing).rows.map { it.pullRequest.number })
        assertEquals(1, fetches)
    }

    private fun refreshedRowsWith(preferences: QueuePreferences, vararg requests: ReviewRequest): List<ReviewRow> = runBlocking {
        val viewModel = viewModelReturning(ReviewRequestsResult.Success(requests.toList()))
        viewModel.usePreferences(preferences)
        viewModel.refresh()
        (viewModel.state.value as Showing).rows
    }

    private fun preferencesWithTiming(nearlyOverdueAfter: Duration, overdueAfter: Duration) =
        QueuePreferences(timing = ReviewTiming(WorkHours.officeHours(ZoneOffset.UTC), nearlyOverdueAfter, overdueAfter))

    private class MovableClock(@Volatile var now: Instant) : Clock() {
        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this
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
        return ReviewQueueViewModel({ remaining.removeFirst() }, recordingOpener, clock, preferences)
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
