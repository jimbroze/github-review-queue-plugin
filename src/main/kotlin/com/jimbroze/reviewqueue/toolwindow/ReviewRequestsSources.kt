package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.github.fetchReviewRequestsForIdeAccount

val ideAccountReviewRequests = ReviewRequestsSource { fetchReviewRequestsForIdeAccount() }
