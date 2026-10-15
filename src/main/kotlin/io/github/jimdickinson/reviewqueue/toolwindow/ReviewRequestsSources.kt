package io.github.jimdickinson.reviewqueue.toolwindow

import io.github.jimdickinson.reviewqueue.github.fetchReviewRequestsForIdeAccount

val ideAccountReviewRequests = ReviewRequestsSource { fetchReviewRequestsForIdeAccount() }
