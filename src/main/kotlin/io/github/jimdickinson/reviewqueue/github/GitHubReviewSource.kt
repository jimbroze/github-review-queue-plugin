package io.github.jimdickinson.reviewqueue.github

import io.github.jimdickinson.reviewqueue.queue.RequestedReviewer
import io.github.jimdickinson.reviewqueue.queue.ReviewRequest
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult
import io.github.jimdickinson.reviewqueue.timeline.PendingReview
import io.github.jimdickinson.reviewqueue.timeline.Viewer
import io.github.jimdickinson.reviewqueue.timeline.pendingReviewFor

class GitHubReviewSource(private val graphQl: GitHubGraphQl) {

    /** Returns every open pull request, across all repositories, awaiting the signed-in user's review. */
    suspend fun fetchReviewRequests(): ReviewRequestsResult {
        val login = graphQl.query(VIEWER_QUERY, emptyMap())
            .answer(::parseViewerLogin)
            .valueOr { return it }
        val reviewQueue = graphQl.query(REVIEW_QUEUE_QUERY, mapOf("login" to login, "searchQuery" to SEARCH_QUERY))
            .answer(::parseReviewQueue)
            .valueOr { return it }
        val viewer = Viewer(login, reviewQueue.teamSlugs)
        return ReviewRequestsResult.Success(reviewQueue.pullRequests.map { reviewRequest(it, viewer) })
    }

    private fun reviewRequest(pullRequest: SearchedPullRequest, viewer: Viewer): ReviewRequest {
        val pending = pendingReviewFor(viewer, pullRequest.timeline) ?: requestedSinceOpened(pullRequest)
        return ReviewRequest(
            pullRequest = pullRequest.ref,
            title = pullRequest.title,
            url = pullRequest.url,
            requestedAt = pending.requestedAt,
            requestedReviewer = pending.requestedReviewer,
            isReReview = pending.isReReview,
        )
    }

    // Search already confirmed the request, so a timeline that does not show it was cut short.
    private fun requestedSinceOpened(pullRequest: SearchedPullRequest) =
        PendingReview(pullRequest.createdAt, RequestedReviewer.You, isReReview = false)

    private companion object {
        const val SEARCH_QUERY = "is:pr is:open archived:false review-requested:@me"

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
