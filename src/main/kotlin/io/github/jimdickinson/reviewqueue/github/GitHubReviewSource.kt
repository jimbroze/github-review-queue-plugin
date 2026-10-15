package io.github.jimdickinson.reviewqueue.github

import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult

class GitHubReviewSource(private val graphQl: GitHubGraphQl) {

    /** Returns every open pull request, across all repositories, awaiting the signed-in user's review. */
    suspend fun fetchReviewRequests(): ReviewRequestsResult = TODO()

    private companion object {
        const val SEARCH_QUERY = "is:pr is:open archived:false review-requested:@me"

        val REVIEW_QUEUE_QUERY = """
            query ReviewQueue(${'$'}searchQuery: String!) {
              viewer {
                login
                organizations(first: 100) {
                  nodes {
                    teams(first: 100, role: MEMBER) { nodes { combinedSlug } }
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
