package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.queue.PullRequestRef
import com.jimbroze.reviewqueue.queue.RequestedReviewer
import com.jimbroze.reviewqueue.queue.ReviewQueue
import com.jimbroze.reviewqueue.queue.ReviewRequest
import com.jimbroze.reviewqueue.queue.ReviewRequestsResult
import com.jimbroze.reviewqueue.queue.ReviewRequestsResult.Failed
import com.jimbroze.reviewqueue.queue.ReviewRequestsResult.NotSignedIn
import com.jimbroze.reviewqueue.queue.ReviewRequestsResult.RateLimited
import com.jimbroze.reviewqueue.queue.ReviewRequestsResult.Success
import com.jimbroze.reviewqueue.queue.ReviewRequestsResult.Unauthorized
import com.jimbroze.reviewqueue.worktime.ReviewTiming
import com.jimbroze.reviewqueue.worktime.Urgency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ReviewQueueViewModel(
    private val source: ReviewRequestsSource,
    private val opener: PullRequestOpener,
    private val clock: Clock,
    private val timing: ReviewTiming,
) {
    private val mutableState = MutableStateFlow<ReviewQueueState>(ReviewQueueState.Loading)
    private val stateLock = Any()
    private var shownRequests: List<ReviewRequest> = emptyList()

    val state: StateFlow<ReviewQueueState> = mutableState.asStateFlow()

    suspend fun refresh() {
        synchronized(stateLock) { mutableState.update(::fetchingState) }
        show(fetchOrFailure())
    }

    /** Recalculates the shown waiting times on each of [ticks], without fetching. */
    suspend fun keepWaitingTimesCurrent(ticks: Flow<Unit>) {
        ticks.collect { showCurrentWaitingTimes() }
    }

    fun open(row: ReviewRow) {
        opener.open(row.pullRequest, row.url)
    }

    private fun fetchingState(current: ReviewQueueState): ReviewQueueState =
        if (current is ReviewQueueState.Showing) current.copy(isRefreshing = true) else ReviewQueueState.Loading

    private suspend fun fetchOrFailure(): ReviewRequestsResult =
        try {
            source.fetch()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (unexpected: Exception) {
            Failed(unexpected.message ?: unexpected.javaClass.simpleName)
        }

    private fun show(result: ReviewRequestsResult) = synchronized(stateLock) {
        shownRequests = if (result is Success) ReviewQueue.order(result.requests) else emptyList()
        mutableState.value = stateFor(result)
    }

    private fun showCurrentWaitingTimes() = synchronized(stateLock) {
        mutableState.update { current ->
            if (current is ReviewQueueState.Showing) current.copy(rows = shownRequests.map(::rowFor)) else current
        }
    }

    private fun stateFor(result: ReviewRequestsResult): ReviewQueueState = when (result) {
        is Success -> stateFor(shownRequests, result.warnings)
        NotSignedIn -> ReviewQueueState.NeedsSignIn
        Unauthorized -> ReviewQueueState.SignInExpired
        is RateLimited -> ReviewQueueState.RateLimited(result.resetAt?.let { timeUntil(it) })
        is Failed -> ReviewQueueState.CouldNotLoad(result.reason)
    }

    private fun stateFor(orderedRequests: List<ReviewRequest>, warnings: List<String>): ReviewQueueState =
        if (orderedRequests.isEmpty()) {
            ReviewQueueState.NothingWaiting(warnings)
        } else {
            ReviewQueueState.Showing(orderedRequests.map(::rowFor), isRefreshing = false, warnings)
        }

    private fun rowFor(request: ReviewRequest): ReviewRow {
        val workingTimeWaited = timing.workHours.workingTimeBetween(request.requestedAt, clock.instant())
        return ReviewRow(
            pullRequestLabel = labelFor(request.pullRequest),
            title = request.title,
            authorLogin = request.authorLogin,
            waitingFor = WorkingTimeText.of(workingTimeWaited, timing.workHours.dayLength),
            urgency = if (request.isDraft) Urgency.OnTime else timing.urgencyAfter(workingTimeWaited),
            requestedFrom = requestedFromText(request.requestedReviewer),
            isReReview = request.isReReview,
            isDraft = request.isDraft,
            pullRequest = request.pullRequest,
            url = request.url,
        )
    }

    private fun labelFor(pullRequest: PullRequestRef): String =
        "${pullRequest.repository.owner}/${pullRequest.repository.name} #${pullRequest.number}"

    private fun requestedFromText(reviewer: RequestedReviewer): String = when (reviewer) {
        RequestedReviewer.You -> "you"
        is RequestedReviewer.Team -> "@${reviewer.slug}"
    }

    private fun timeUntil(moment: Instant): String = DurationText.of(Duration.between(clock.instant(), moment))
}
