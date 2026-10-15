package io.github.jimdickinson.reviewqueue.github

import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.github.api.GithubServerPath
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class IdeAccountReviewRequestsTest {

    private val gitHubDotCom = GithubAccount("octocat", GithubServerPath.DEFAULT_SERVER)
    private val enterprise = GithubAccount("octocat", GithubServerPath("github.acme.com"))
    private val connections = mutableListOf<Connection>()

    private data class Connection(val endpoint: URI, val token: String)

    @Test
    fun `with no GitHub account in the IDE you are not signed in`() {
        assertEquals(ReviewRequestsResult.NotSignedIn, fetch(accounts = emptyList()))
    }

    @Test
    fun `an account without a stored token is not signed in`() {
        assertEquals(ReviewRequestsResult.NotSignedIn, fetch(listOf(gitHubDotCom), tokens = emptyMap()))
    }

    @Test
    fun `nothing is asked of GitHub when you are not signed in`() {
        fetch(listOf(gitHubDotCom), tokens = emptyMap())

        assertEquals(emptyList<Connection>(), connections)
    }

    @Test
    fun `review requests are fetched as the signed-in account`() {
        val result = fetch(listOf(gitHubDotCom), tokens = mapOf(gitHubDotCom to "dotcom-token"))

        assertEquals(ReviewRequestsResult.Success(emptyList()), result)
        assertEquals(listOf(Connection(URI("https://api.github.com/graphql"), "dotcom-token")), connections)
    }

    @Test
    fun `an enterprise account is queried at its own server`() {
        fetch(listOf(enterprise), tokens = mapOf(enterprise to "enterprise-token"))

        assertEquals(listOf(Connection(URI("https://github.acme.com/api/graphql"), "enterprise-token")), connections)
    }

    @Test
    fun `a github dot com account is preferred over other accounts`() {
        fetch(listOf(enterprise, gitHubDotCom), tokens = mapOf(enterprise to "enterprise-token", gitHubDotCom to "dotcom-token"))

        assertEquals(listOf(Connection(URI("https://api.github.com/graphql"), "dotcom-token")), connections)
    }

    @Test
    fun `without a github dot com account the first account is used`() {
        val otherEnterprise = GithubAccount("octocat", GithubServerPath("github.globex.com"))

        fetch(listOf(enterprise, otherEnterprise), tokens = mapOf(enterprise to "acme-token", otherEnterprise to "globex-token"))

        assertEquals(listOf(Connection(URI("https://github.acme.com/api/graphql"), "acme-token")), connections)
    }

    private fun fetch(accounts: List<GithubAccount>, tokens: Map<GithubAccount, String> = emptyMap()) = runBlocking {
        fetchReviewRequestsFor(accounts, findToken = { tokens[it] }, transportFor = ::connect)
    }

    private fun connect(endpoint: URI, token: String): GitHubGraphQl {
        connections += Connection(endpoint, token)
        return FakeGitHubGraphQl(viewerResponse("octocat"), reviewQueueResponse(emptyList()))
    }
}
