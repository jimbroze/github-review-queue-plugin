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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val state: StateFlow<ReviewQueueState> = mutableState.asStateFlow()

    suspend fun refresh() {
        mutableState.value = fetchingState(mutableState.value)
        mutableState.value = stateFor(fetchOrFailure())
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

    private fun stateFor(result: ReviewRequestsResult): ReviewQueueState = when (result) {
        is Success -> stateFor(result.requests, result.warnings)
        NotSignedIn -> ReviewQueueState.NeedsSignIn
        Unauthorized -> ReviewQueueState.SignInExpired
        is RateLimited -> ReviewQueueState.RateLimited(result.resetAt?.let { timeUntil(it) })
        is Failed -> ReviewQueueState.CouldNotLoad(result.reason)
    }

    private fun stateFor(requests: List<ReviewRequest>, warnings: List<String>): ReviewQueueState =
        if (requests.isEmpty()) {
            ReviewQueueState.NothingWaiting(warnings)
        } else {
            ReviewQueueState.Showing(ReviewQueue.order(requests).map(::rowFor), isRefreshing = false, warnings)
        }

    private fun rowFor(request: ReviewRequest) = ReviewRow(
        pullRequestLabel = labelFor(request.pullRequest),
        title = request.title,
        authorLogin = request.authorLogin,
        waitingFor = workingTimeSince(request.requestedAt),
        requestedFrom = requestedFromText(request.requestedReviewer),
        isReReview = request.isReReview,
        isDraft = request.isDraft,
        pullRequest = request.pullRequest,
        url = request.url,
    )

    private fun labelFor(pullRequest: PullRequestRef): String =
        "${pullRequest.repository.owner}/${pullRequest.repository.name} #${pullRequest.number}"

    private fun requestedFromText(reviewer: RequestedReviewer): String = when (reviewer) {
        RequestedReviewer.You -> "you"
        is RequestedReviewer.Team -> "@${reviewer.slug}"
    }

    private fun workingTimeSince(moment: Instant): String {
        val workHours = timing.workHours
        return WorkingTimeText.of(workHours.workingTimeBetween(moment, clock.instant()), workHours.dayLength)
    }

    private fun timeUntil(moment: Instant): String = DurationText.of(Duration.between(clock.instant(), moment))
}
