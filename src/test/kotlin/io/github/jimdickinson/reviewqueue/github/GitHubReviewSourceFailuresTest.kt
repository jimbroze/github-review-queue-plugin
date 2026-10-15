package io.github.jimdickinson.reviewqueue.github

import io.github.jimdickinson.reviewqueue.queue.RequestedReviewer
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GitHubReviewSourceFailuresTest {

    private val viewer = GraphQlResponse.Body(viewerResponse("octocat"))

    @Test
    fun `a token GitHub rejects is reported as unauthorized`() {
        assertEquals(ReviewRequestsResult.Unauthorized, fetch(GraphQlResponse.Unauthorized))
    }

    @Test
    fun `hitting GitHub's rate limit is reported with when the limit resets`() {
        val resetAt = Instant.parse("2026-01-01T10:00:00Z")

        assertEquals(ReviewRequestsResult.RateLimited(resetAt), fetch(GraphQlResponse.RateLimited(resetAt)))
    }

    @Test
    fun `a request that cannot reach GitHub is reported as failed with its reason`() {
        assertEquals(
            ReviewRequestsResult.Failed("Connection refused"),
            fetch(GraphQlResponse.Failed("Connection refused")),
        )
    }

    @Test
    fun `a failure fetching the review queue after your login is reported the same way`() {
        assertEquals(ReviewRequestsResult.Unauthorized, fetch(viewer, GraphQlResponse.Unauthorized))
    }

    @Test
    fun `nothing more is asked of GitHub once a request fails`() {
        val graphQl = FakeGitHubGraphQl(GraphQlResponse.Unauthorized)

        runBlocking { GitHubReviewSource(graphQl).fetchReviewRequests() }

        assertEquals(1, graphQl.receivedVariables.size)
    }

    @Test
    fun `a rate limit error in GitHub's answer is reported as rate limited without a known reset time`() {
        val rateLimitError = GraphQlResponse.Body(errorResponse("RATE_LIMITED", "API rate limit exceeded"))

        assertEquals(ReviewRequestsResult.RateLimited(resetAt = null), fetch(viewer, rateLimitError))
    }

    @Test
    fun `any other error in GitHub's answer is reported as failed with GitHub's message`() {
        val error = GraphQlResponse.Body(errorResponse("NOT_FOUND", "Could not resolve to a User"))

        assertEquals(ReviewRequestsResult.Failed("Could not resolve to a User"), fetch(error))
    }

    @Test
    fun `an answer that is not the expected shape is reported as failed instead of throwing`() {
        val result = fetch(GraphQlResponse.Body("<html>Unicorn!</html>"))

        assertTrue(result is ReviewRequestsResult.Failed)
    }

    @Test
    fun `an error alongside data that came back still shows the pull requests that came back`() {
        val partialQueue = withError(
            reviewQueueResponse(
                listOf("acme/platform"),
                pullRequest(timeline = listOf(reviewRequested("2026-01-02T10:00:00Z", team("acme/platform")))),
                hiddenOrganisations = 1,
            ),
            type = "FORBIDDEN",
            message = "Resource protected by organization SAML enforcement.",
        )

        val result = fetch(viewer, GraphQlResponse.Body(partialQueue))

        val requestedReviewers = (result as ReviewRequestsResult.Success).requests.map { it.requestedReviewer }
        assertEquals(listOf(RequestedReviewer.Team("acme/platform")), requestedReviewers)
    }

    @Test
    fun `a rate limit error alongside data that came back is still reported as rate limited`() {
        val partialQueue = withError(reviewQueueResponse(emptyList()), type = "RATE_LIMITED", message = "API rate limit exceeded")

        assertEquals(ReviewRequestsResult.RateLimited(resetAt = null), fetch(viewer, GraphQlResponse.Body(partialQueue)))
    }

    private fun fetch(vararg responses: GraphQlResponse): ReviewRequestsResult =
        runBlocking { GitHubReviewSource(FakeGitHubGraphQl(*responses)).fetchReviewRequests() }
}
