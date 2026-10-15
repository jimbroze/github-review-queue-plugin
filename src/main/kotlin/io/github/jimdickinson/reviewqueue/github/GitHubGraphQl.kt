package io.github.jimdickinson.reviewqueue.github

/** Sends GraphQL requests to GitHub as the signed-in user. */
interface GitHubGraphQl {
    /** Returns the raw JSON response body for [query] run with [variables]. */
    suspend fun query(query: String, variables: Map<String, Any?>): String
}
