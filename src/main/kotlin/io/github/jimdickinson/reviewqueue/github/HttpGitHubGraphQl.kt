package io.github.jimdickinson.reviewqueue.github

import java.net.URI
import java.net.http.HttpClient
import java.time.Clock
import java.time.Duration

/** Sends GraphQL requests over HTTP to [endpoint], authenticated with [token]. */
class HttpGitHubGraphQl(
    private val endpoint: URI,
    private val token: String,
    private val clock: Clock = Clock.systemUTC(),
    private val timeout: Duration = Duration.ofSeconds(30),
    private val client: HttpClient = HttpClient.newHttpClient(),
) : GitHubGraphQl {

    override suspend fun query(query: String, variables: Map<String, Any?>): GraphQlResponse = TODO("Step 4b-ii")
}
