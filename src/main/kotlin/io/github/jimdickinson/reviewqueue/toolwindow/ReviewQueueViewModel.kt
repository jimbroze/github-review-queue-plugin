package io.github.jimdickinson.reviewqueue.toolwindow

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.queue.RequestedReviewer
import io.github.jimdickinson.reviewqueue.queue.ReviewQueue
import io.github.jimdickinson.reviewqueue.queue.ReviewRequest
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult.Failed
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult.NotSignedIn
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult.RateLimited
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult.Success
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult.Unauthorized
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
        is Success -> stateFor(result.requests)
        NotSignedIn -> ReviewQueueState.NeedsSignIn
        Unauthorized -> ReviewQueueState.SignInExpired
        is RateLimited -> ReviewQueueState.RateLimited(result.resetAt?.let { timeUntil(it) })
        is Failed -> ReviewQueueState.CouldNotLoad(result.reason)
    }

    private fun stateFor(requests: List<ReviewRequest>): ReviewQueueState =
        if (requests.isEmpty()) {
            ReviewQueueState.NothingWaiting
        } else {
            ReviewQueueState.Showing(ReviewQueue.order(requests).map(::rowFor), isRefreshing = false)
        }

    private fun rowFor(request: ReviewRequest) = ReviewRow(
        pullRequestLabel = labelFor(request.pullRequest),
        title = request.title,
        authorLogin = "",
        waitingFor = timeSince(request.requestedAt),
        requestedFrom = requestedFromText(request.requestedReviewer),
        isReReview = request.isReReview,
        isDraft = false,
        pullRequest = request.pullRequest,
        url = request.url,
    )

    private fun labelFor(pullRequest: PullRequestRef): String =
        "${pullRequest.repository.owner}/${pullRequest.repository.name} #${pullRequest.number}"

    private fun requestedFromText(reviewer: RequestedReviewer): String = when (reviewer) {
        RequestedReviewer.You -> "you"
        is RequestedReviewer.Team -> "@${reviewer.slug}"
    }

    private fun timeSince(moment: Instant): String = DurationText.of(Duration.between(moment, clock.instant()))

    private fun timeUntil(moment: Instant): String = DurationText.of(Duration.between(clock.instant(), moment))
}
