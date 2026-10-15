package io.github.jimdickinson.reviewqueue.github

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class HttpGitHubGraphQlTest {

    private val now = Instant.parse("2026-01-01T09:00:00Z")
    private val ok = CannedResponse(200, """{ "data": {} }""")

    @Test
    fun `github dot com is queried through its API host`() {
        assertEquals(URI("https://api.github.com/graphql"), graphQlEndpoint("github.com"))
    }

    @Test
    fun `a GitHub Enterprise server is queried at its own GraphQL path`() {
        assertEquals(URI("https://github.acme.com/api/graphql"), graphQlEndpoint("github.acme.com"))
    }

    @Test
    fun `the query is posted to the endpoint`() {
        val request = requestSentFor(ok)

        assertEquals("POST", request.method)
        assertEquals("/graphql", request.path)
    }

    @Test
    fun `the token is sent as a bearer token`() {
        assertEquals("Bearer secret-token", requestSentFor(ok).headers["authorization"])
    }

    @Test
    fun `the request asks for JSON and names the plugin as its user agent`() {
        val headers = requestSentFor(ok).headers

        assertEquals("application/json", headers["accept"])
        assertTrue(headers["user-agent"].orEmpty().contains("Review Queue"))
    }

    @Test
    fun `the query and its variables are sent as a JSON body`() {
        val variables = mapOf(
            "login" to "octocat",
            "after" to null,
            "first" to 100,
            "logins" to listOf("octocat", "hubot"),
            "filter" to mapOf("state" to "OPEN"),
        )

        val request = requestSentFor(ok, query = "query Viewer { viewer { login } }", variables = variables)

        val expected = """
            {
              "query": "query Viewer { viewer { login } }",
              "variables": {
                "login": "octocat",
                "after": null,
                "first": 100,
                "logins": ["octocat", "hubot"],
                "filter": { "state": "OPEN" }
              }
            }
        """
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(request.body))
    }

    @Test
    fun `a successful response gives its body`() {
        val body = """{ "data": { "viewer": { "login": "octocat" } } }"""

        assertEquals(GraphQlResponse.Body(body), responseFor(CannedResponse(200, body)))
    }

    @Test
    fun `a rejected token is unauthorized`() {
        assertEquals(GraphQlResponse.Unauthorized, responseFor(CannedResponse(401, """{ "message": "Bad credentials" }""")))
    }

    @Test
    fun `a forbidden response with no requests remaining is rate limited until the reset time given`() {
        val resetAt = Instant.parse("2026-01-01T10:00:00Z")
        val response = CannedResponse(
            403,
            headers = mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to resetAt.epochSecond.toString()),
        )

        assertEquals(GraphQlResponse.RateLimited(resetAt), responseFor(response))
    }

    @Test
    fun `a response asking to retry later is rate limited until that many seconds from now`() {
        val response = CannedResponse(403, headers = mapOf("retry-after" to "60"))

        assertEquals(GraphQlResponse.RateLimited(now.plusSeconds(60)), responseFor(response))
    }

    @Test
    fun `a too-many-requests response without timing headers is rate limited with no known reset time`() {
        assertEquals(GraphQlResponse.RateLimited(resetAt = null), responseFor(CannedResponse(429)))
    }

    @Test
    fun `a forbidden response that is not a rate limit fails`() {
        assertTrue(responseFor(CannedResponse(403, headers = mapOf("x-ratelimit-remaining" to "4999"))) is GraphQlResponse.Failed)
    }

    @Test
    fun `any other error status fails naming the status`() {
        val response = responseFor(CannedResponse(502, "Bad Gateway"))

        assertTrue((response as GraphQlResponse.Failed).reason.contains("502"))
    }

    @Test
    fun `a server that cannot be reached fails instead of throwing`() {
        val stoppedServerUri = LocalHttpServer(ok).use { it.graphQlUri }

        val response = runBlocking { HttpGitHubGraphQl(stoppedServerUri, "secret-token").query("query", emptyMap()) }

        assertTrue(response is GraphQlResponse.Failed)
    }

    @Test
    fun `a server that does not answer in time fails instead of throwing`() {
        val response = responseFor(ok.slowedBy(millis = 2_000), timeout = Duration.ofMillis(200))

        assertTrue(response is GraphQlResponse.Failed)
    }

    private fun CannedResponse.slowedBy(millis: Long) = CannedResponse(status, body, headers, delayMillis = millis)

    private fun requestSentFor(
        response: CannedResponse,
        query: String = "query Viewer { viewer { login } }",
        variables: Map<String, Any?> = emptyMap(),
    ): ReceivedRequest = LocalHttpServer(response).use { server ->
        runBlocking { transport(server.graphQlUri).query(query, variables) }
        server.received!!
    }

    private fun responseFor(response: CannedResponse, timeout: Duration = Duration.ofSeconds(5)): GraphQlResponse =
        LocalHttpServer(response).use { server ->
            runBlocking { transport(server.graphQlUri, timeout).query("query Viewer { viewer { login } }", emptyMap()) }
        }

    private fun transport(endpoint: URI, timeout: Duration = Duration.ofSeconds(5)) =
        HttpGitHubGraphQl(endpoint, "secret-token", Clock.fixed(now, ZoneOffset.UTC), timeout)
}
