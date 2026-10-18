package com.jimbroze.reviewqueue.github

import java.time.Instant

/** Sends GraphQL requests to GitHub as the signed-in user. */
interface GitHubGraphQl {
    /** Runs [query] with [variables]; never throws, reporting transport failures as a [GraphQlResponse] instead. */
    suspend fun query(query: String, variables: Map<String, Any?>): GraphQlResponse
}

sealed interface GraphQlResponse {
    /** GitHub answered; [json] is the raw response body, which may still carry GraphQL errors. */
    data class Body(val json: String) : GraphQlResponse
    data object Unauthorized : GraphQlResponse
    data class RateLimited(val resetAt: Instant?) : GraphQlResponse
    data class Failed(val reason: String) : GraphQlResponse
}
