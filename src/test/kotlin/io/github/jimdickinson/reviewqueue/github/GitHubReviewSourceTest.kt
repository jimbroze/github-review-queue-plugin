package io.github.jimdickinson.reviewqueue.github

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import io.github.jimdickinson.reviewqueue.queue.RequestedReviewer
import io.github.jimdickinson.reviewqueue.queue.ReviewRequest
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GitHubReviewSourceTest {

    private val you = "octocat"
    private val yourTeams = listOf("acme/platform", "acme/security")

    @Test
    fun `searches GitHub for open pull requests in unarchived repositories awaiting your review`() {
        val graphQl = fakeGitHub()

        runBlocking { GitHubReviewSource(graphQl).fetchReviewRequests() }

        assertTrue(graphQl.receivedVariables.any { "is:pr is:open archived:false review-requested:@me -assignee:@me" in it.values })
    }

    @Test
    fun `your teams are looked up by your login`() {
        val graphQl = fakeGitHub()

        runBlocking { GitHubReviewSource(graphQl).fetchReviewRequests() }

        assertTrue(graphQl.receivedVariables.last().containsValue(you))
    }

    @Test
    fun `no pull requests awaiting your review gives an empty queue`() {
        assertEquals(ReviewRequestsResult.Success(emptyList()), fetch())
    }

    @Test
    fun `a pull request requesting your review directly is listed with its details`() {
        val result = fetch(
            pullRequest(
                owner = "acme", repository = "rocket", number = 42, title = "Add thrusters", author = "alice",
                timeline = listOf(reviewRequested("2026-01-02T10:00:00Z", user(you))),
            ),
        )

        val expected = ReviewRequest(
            pullRequest = PullRequestRef(RepositoryName("acme", "rocket"), 42),
            title = "Add thrusters",
            authorLogin = "alice",
            url = "https://github.com/acme/rocket/pull/42",
            requestedAt = Instant.parse("2026-01-02T10:00:00Z"),
            requestedReviewer = RequestedReviewer.You,
            isReReview = false,
            isDraft = false,
        )
        assertEquals(ReviewRequestsResult.Success(listOf(expected)), result)
    }

    @Test
    fun `a pull request requesting a team you belong to is listed as that team with its organisation`() {
        val request = fetchSingle(
            pullRequest(timeline = listOf(reviewRequested("2026-01-02T10:00:00Z", team("acme/platform")))),
        )

        assertEquals(RequestedReviewer.Team("acme/platform"), request.requestedReviewer)
    }

    @Test
    fun `a team in another organisation with the same name as yours is not your team`() {
        val request = fetchSingle(
            pullRequest(
                timeline = listOf(
                    reviewRequested("2026-01-02T10:00:00Z", team("other-org/platform")),
                    reviewRequested("2026-01-03T10:00:00Z", team("acme/security")),
                ),
            ),
        )

        assertEquals(RequestedReviewer.Team("acme/security"), request.requestedReviewer)
        assertEquals(Instant.parse("2026-01-03T10:00:00Z"), request.requestedAt)
    }

    @Test
    fun `a request removed from the timeline is no longer the one you are waited on for`() {
        val request = fetchSingle(
            pullRequest(
                timeline = listOf(
                    reviewRequested("2026-01-02T10:00:00Z", user(you)),
                    reviewRequestRemoved("2026-01-02T11:00:00Z", user(you)),
                    reviewRequested("2026-01-04T10:00:00Z", user(you)),
                ),
            ),
        )

        assertEquals(Instant.parse("2026-01-04T10:00:00Z"), request.requestedAt)
    }

    @Test
    fun `being asked to review again after submitting a review is a re-review`() {
        val request = fetchSingle(
            pullRequest(
                timeline = listOf(
                    reviewRequested("2026-01-02T10:00:00Z", user(you)),
                    submittedReview("2026-01-03T10:00:00Z", author = you),
                    reviewRequested("2026-01-04T10:00:00Z", user(you)),
                ),
            ),
        )

        assertEquals(true, request.isReReview)
        assertEquals(Instant.parse("2026-01-04T10:00:00Z"), request.requestedAt)
    }

    @Test
    fun `a draft review you have not submitted does not count as reviewing`() {
        val request = fetchSingle(
            pullRequest(
                timeline = listOf(
                    reviewRequested("2026-01-02T10:00:00Z", user(you)),
                    draftReview(author = you),
                ),
            ),
        )

        assertEquals(false, request.isReReview)
        assertEquals(Instant.parse("2026-01-02T10:00:00Z"), request.requestedAt)
    }

    @Test
    fun `requests to reviewers that are neither users nor teams are ignored`() {
        val request = fetchSingle(
            pullRequest(
                timeline = listOf(
                    reviewRequested("2026-01-02T10:00:00Z", mannequin()),
                    reviewRequested("2026-01-02T11:00:00Z", DELETED_REVIEWER),
                    reviewRequested("2026-01-03T10:00:00Z", user(you)),
                ),
            ),
        )

        assertEquals(RequestedReviewer.You, request.requestedReviewer)
        assertEquals(Instant.parse("2026-01-03T10:00:00Z"), request.requestedAt)
    }

    @Test
    fun `every pull request found is listed in the order GitHub returned them`() {
        val result = fetch(
            pullRequest(owner = "acme", repository = "rocket", number = 1, timeline = listOf(requestedFromYou())),
            pullRequest(owner = "globex", repository = "engine", number = 2, timeline = listOf(requestedFromYou())),
        )

        val listed = (result as ReviewRequestsResult.Success).requests.map { it.pullRequest }
        assertEquals(
            listOf(
                PullRequestRef(RepositoryName("acme", "rocket"), 1),
                PullRequestRef(RepositoryName("globex", "engine"), 2),
            ),
            listed,
        )
    }

    @Test
    fun `a pull request whose timeline does not show your request is still listed as a direct request since it was opened`() {
        val request = fetchSingle(
            pullRequest(
                createdAt = "2026-01-01T09:00:00Z",
                timeline = listOf(reviewRequested("2026-01-02T10:00:00Z", user("hubot"))),
            ),
        )

        assertEquals(RequestedReviewer.You, request.requestedReviewer)
        assertEquals(Instant.parse("2026-01-01T09:00:00Z"), request.requestedAt)
        assertEquals(false, request.isReReview)
    }

    @Test
    fun `pull requests assigned to you are left out of the search`() {
        val graphQl = fakeGitHub()

        runBlocking { GitHubReviewSource(graphQl).fetchReviewRequests() }

        val searchTerms = graphQl.receivedVariables.flatMap { it.values }.filterIsInstance<String>().flatMap { it.split(" ") }
        assertTrue("-assignee:@me" in searchTerms)
    }

    @Test
    fun `a draft pull request is marked as a draft`() {
        val request = fetchSingle(pullRequest(isDraft = true, timeline = listOf(requestedFromYou())))

        assertEquals(true, request.isDraft)
    }

    @Test
    fun `a pull request ready for review is not a draft`() {
        val request = fetchSingle(pullRequest(isDraft = false, timeline = listOf(requestedFromYou())))

        assertEquals(false, request.isDraft)
    }

    @Test
    fun `a pull request is listed with its author's login`() {
        val request = fetchSingle(pullRequest(author = "alice", timeline = listOf(requestedFromYou())))

        assertEquals("alice", request.authorLogin)
    }

    @Test
    fun `a pull request whose author's account was deleted is listed as by ghost`() {
        val request = fetchSingle(pullRequest(author = null, timeline = listOf(requestedFromYou())))

        assertEquals("ghost", request.authorLogin)
    }

    private fun requestedFromYou() = reviewRequested("2026-01-02T10:00:00Z", user(you))

    private fun fetch(vararg pullRequests: String): ReviewRequestsResult {
        val graphQl = fakeGitHub(*pullRequests)
        return runBlocking { GitHubReviewSource(graphQl).fetchReviewRequests() }
    }

    private fun fakeGitHub(vararg pullRequests: String) =
        FakeGitHubGraphQl(viewerResponse(you), reviewQueueResponse(yourTeams, *pullRequests))

    private fun fetchSingle(pullRequest: String): ReviewRequest =
        (fetch(pullRequest) as ReviewRequestsResult.Success).requests.single()
}
