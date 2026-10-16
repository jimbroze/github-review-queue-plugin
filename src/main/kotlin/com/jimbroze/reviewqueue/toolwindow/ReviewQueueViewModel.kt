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
import java.util.concurrent.atomic.AtomicInteger

class ReviewQueueViewModel(
    private val source: ReviewRequestsSource,
    private val clock: Clock,
    private var preferences: QueuePreferences,
) {
    private val mutableState = MutableStateFlow<ReviewQueueState>(ReviewQueueState.Loading)
    private val stateLock = Any()
    private val runningFetches = AtomicInteger()
    private var loadedQueue: Success? = null

    val state: StateFlow<ReviewQueueState> = mutableState.asStateFlow()

    suspend fun refresh() {
        runningFetches.incrementAndGet()
        try {
            synchronized(stateLock) { mutableState.update(::fetchingState) }
            show(fetchOrFailure())
        } finally {
            runningFetches.decrementAndGet()
        }
    }

    /** Recalculates the shown waiting times on each of [ticks], without fetching. */
    suspend fun keepWaitingTimesCurrent(ticks: Flow<Unit>) {
        ticks.collect { showCurrentWaitingTimes() }
    }

    /** Fetches again on each of [ticks], skipping a tick while a fetch is still running. */
    suspend fun keepRefreshing(ticks: Flow<Unit>) {
        ticks.collect { if (runningFetches.get() == 0) refresh() }
    }

    /** Shows the loaded queue with [newPreferences], without fetching. */
    fun usePreferences(newPreferences: QueuePreferences) = synchronized(stateLock) {
        preferences = newPreferences
        showLoadedQueueAgain()
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
        loadedQueue = result as? Success
        mutableState.value = stateFor(result)
    }

    private fun showCurrentWaitingTimes() = synchronized(stateLock) {
        showLoadedQueueAgain()
    }

    private fun showLoadedQueueAgain() {
        val queue = loadedQueue ?: return
        mutableState.update { current ->
            when (current) {
                is ReviewQueueState.Showing -> stateFor(queue, isRefreshing = current.isRefreshing)
                is ReviewQueueState.NothingWaiting -> stateFor(queue)
                else -> current
            }
        }
    }

    private fun stateFor(result: ReviewRequestsResult): ReviewQueueState = when (result) {
        is Success -> stateFor(result)
        NotSignedIn -> ReviewQueueState.NeedsSignIn
        Unauthorized -> ReviewQueueState.SignInExpired
        is RateLimited -> ReviewQueueState.RateLimited(result.resetAt?.let { timeUntil(it) })
        is Failed -> ReviewQueueState.CouldNotLoad(result.reason)
    }

    private fun stateFor(queue: Success, isRefreshing: Boolean = false): ReviewQueueState {
        val shownRequests = ReviewQueue.order(preferences.filter.applyTo(queue.requests), preferences.sortOrder)
        return if (shownRequests.isEmpty()) {
            ReviewQueueState.NothingWaiting(queue.warnings)
        } else {
            ReviewQueueState.Showing(shownRequests.map(::rowFor), isRefreshing, queue.warnings)
        }
    }

    private fun rowFor(request: ReviewRequest): ReviewRow {
        val workingTimeWaited = preferences.timing.workHours.workingTimeBetween(request.requestedAt, clock.instant())
        return ReviewRow(
            pullRequestLabel = labelFor(request.pullRequest),
            title = request.title,
            authorLogin = request.authorLogin,
            waitingFor = WorkingTimeText.of(workingTimeWaited, preferences.timing.workHours.dayLength),
            urgency = if (request.isDraft) Urgency.OnTime else preferences.timing.urgencyAfter(workingTimeWaited),
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
