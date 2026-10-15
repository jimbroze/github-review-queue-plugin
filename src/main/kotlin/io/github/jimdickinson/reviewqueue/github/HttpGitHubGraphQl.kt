package io.github.jimdickinson.reviewqueue.github

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** Sends GraphQL requests over HTTP to [endpoint], authenticated with [token]. */
class HttpGitHubGraphQl(
    private val endpoint: URI,
    private val token: String,
    private val clock: Clock = Clock.systemUTC(),
    private val timeout: Duration = Duration.ofSeconds(30),
    private val client: HttpClient = HttpClient.newHttpClient(),
) : GitHubGraphQl {

    override suspend fun query(query: String, variables: Map<String, Any?>): GraphQlResponse = try {
        responseFor(client.sendAsync(request(query, variables), HttpResponse.BodyHandlers.ofString()).await())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (requestFailure: Exception) {
        GraphQlResponse.Failed("Request to GitHub failed: ${requestFailure.message ?: requestFailure.javaClass.simpleName}")
    }

    private fun request(query: String, variables: Map<String, Any?>): HttpRequest {
        val body = JsonObject(mapOf("query" to JsonPrimitive(query), "variables" to toJson(variables)))
        return HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", USER_AGENT)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()
    }

    private fun responseFor(response: HttpResponse<String>): GraphQlResponse = when {
        response.statusCode() in 200..299 -> GraphQlResponse.Body(response.body())
        response.statusCode() == 401 -> GraphQlResponse.Unauthorized
        isRateLimit(response) -> GraphQlResponse.RateLimited(rateLimitResetAt(response))
        else -> GraphQlResponse.Failed("GitHub returned HTTP ${response.statusCode()}")
    }

    // GitHub signals its primary rate limit with a 403 and no requests remaining,
    // and its secondary rate limit with a 403 or 429 carrying `retry-after`.
    private fun isRateLimit(response: HttpResponse<String>): Boolean =
        response.statusCode() == 429 ||
            (response.statusCode() == 403 && hasRateLimitHeaders(response))

    private fun hasRateLimitHeaders(response: HttpResponse<String>): Boolean =
        response.header("x-ratelimit-remaining") == "0" || response.header("retry-after") != null

    // `x-ratelimit-reset` is in epoch seconds; `retry-after` is in seconds from now.
    private fun rateLimitResetAt(response: HttpResponse<String>): Instant? =
        response.header("x-ratelimit-reset")?.toLongOrNull()?.let(Instant::ofEpochSecond)
            ?: response.header("retry-after")?.toLongOrNull()?.let { clock.instant().plusSeconds(it) }

    private fun HttpResponse<String>.header(name: String): String? = headers().firstValue(name).orElse(null)

    private fun toJson(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map(::toJson))
        is Map<*, *> -> JsonObject(value.entries.associate { (key, item) -> key.toString() to toJson(item) })
        else -> throw IllegalArgumentException("Cannot send ${value::class.simpleName} as a GraphQL variable")
    }

    private companion object {
        const val USER_AGENT = "Review Queue JetBrains plugin"
    }
}
