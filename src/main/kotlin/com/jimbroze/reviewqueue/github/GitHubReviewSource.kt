package com.jimbroze.reviewqueue.github

import com.jimbroze.reviewqueue.queue.RequestedReviewer
import com.jimbroze.reviewqueue.queue.ReviewRequest
import com.jimbroze.reviewqueue.queue.ReviewRequestsResult
import com.jimbroze.reviewqueue.timeline.PendingReview
import com.jimbroze.reviewqueue.timeline.Viewer
import com.jimbroze.reviewqueue.timeline.pendingReviewFor

class GitHubReviewSource(private val graphQl: GitHubGraphQl) {

    /** Returns every open pull request, across all repositories, awaiting the signed-in user's review. */
    suspend fun fetchReviewRequests(): ReviewRequestsResult {
        val login = graphQl.query(VIEWER_QUERY, emptyMap())
            .answer(::parseViewerLogin)
            .answeredOr { return it }
        val reviewQueue = graphQl.query(REVIEW_QUEUE_QUERY, mapOf("login" to login.value, "searchQuery" to SEARCH_QUERY))
            .answer(::parseReviewQueue)
            .answeredOr { return it }
        val viewer = Viewer(login.value, reviewQueue.value.teamSlugs)
        return ReviewRequestsResult.Success(
            requests = reviewQueue.value.pullRequests.map { reviewRequest(it, viewer) },
            warnings = (login.warnings + reviewQueue.warnings).distinct(),
        )
    }

    private fun reviewRequest(pullRequest: SearchedPullRequest, viewer: Viewer): ReviewRequest {
        val pending = pendingReviewFor(viewer, pullRequest.timeline) ?: requestedSinceOpened(pullRequest)
        return ReviewRequest(
            pullRequest = pullRequest.ref,
            title = pullRequest.title,
            authorLogin = pullRequest.authorLogin,
            url = pullRequest.url,
            requestedAt = pending.requestedAt,
            requestedReviewer = pending.requestedReviewer,
            isReReview = pending.isReReview,
            isDraft = pullRequest.isDraft,
        )
    }

    // Search already confirmed the request, so a timeline that does not show it was cut short.
    private fun requestedSinceOpened(pullRequest: SearchedPullRequest) =
        PendingReview(pullRequest.createdAt, RequestedReviewer.You, isReReview = false)

    private companion object {
        const val SEARCH_QUERY = "is:pr is:open archived:false review-requested:@me -assignee:@me"

        const val VIEWER_QUERY = "query Viewer { viewer { login } }"

        val REVIEW_QUEUE_QUERY = """
            query ReviewQueue(${'$'}login: String!, ${'$'}searchQuery: String!) {
              viewer {
                organizations(first: 100) {
                  nodes {
                    teams(first: 100, userLogins: [${'$'}login]) { nodes { combinedSlug } }
                  }
                }
              }
              search(type: ISSUE, query: ${'$'}searchQuery, first: 100) {
                nodes {
                  ... on PullRequest {
                    number
                    title
                    isDraft
                    author { login }
                    url
                    createdAt
                    repository { name owner { login } }
                    timelineItems(last: 100, itemTypes: [REVIEW_REQUESTED_EVENT, REVIEW_REQUEST_REMOVED_EVENT, PULL_REQUEST_REVIEW]) {
                      nodes {
                        __typename
                        ... on ReviewRequestedEvent { createdAt requestedReviewer { ...Reviewer } }
                        ... on ReviewRequestRemovedEvent { createdAt requestedReviewer { ...Reviewer } }
                        ... on PullRequestReview { state submittedAt author { login } }
                      }
                    }
                  }
                }
              }
            }

            fragment Reviewer on RequestedReviewer {
              __typename
              ... on User { login }
              ... on Team { combinedSlug }
            }
        """.trimIndent()
    }
}
