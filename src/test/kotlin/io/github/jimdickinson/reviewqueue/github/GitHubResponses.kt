package io.github.jimdickinson.reviewqueue.github

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class FakeGitHubGraphQl(vararg responsesInOrder: GraphQlResponse) : GitHubGraphQl {
    private val responses = ArrayDeque(responsesInOrder.toList())
    val receivedVariables = mutableListOf<Map<String, Any?>>()

    constructor(vararg bodiesInOrder: String) : this(*bodiesInOrder.map(GraphQlResponse::Body).toTypedArray())

    override suspend fun query(query: String, variables: Map<String, Any?>): GraphQlResponse {
        receivedVariables += variables
        return responses.removeFirst()
    }
}

fun viewerResponse(login: String) = """{ "data": { "viewer": { "login": "$login" } } }"""

fun errorResponse(type: String, message: String) =
    """{ "data": null, "errors": [{ "type": "$type", "message": "$message" }] }"""

fun withError(response: String, type: String, message: String): String {
    val error = Json.parseToJsonElement("""[{ "type": "$type", "message": "$message" }]""")
    return JsonObject(Json.parseToJsonElement(response).jsonObject + ("errors" to error)).toString()
}

fun reviewQueueResponse(viewerTeams: List<String>, vararg pullRequests: String, hiddenOrganisations: Int = 0) = """
    {
      "data": {
        "viewer": {
          "organizations": {
            "nodes": [
              ${"null, ".repeat(hiddenOrganisations)}{ "teams": { "nodes": [${viewerTeams.joinToString { """{ "combinedSlug": "$it" }""" }}] } }
            ]
          }
        },
        "search": { "nodes": [${pullRequests.joinToString()}] }
      }
    }
"""

fun pullRequest(
    owner: String = "acme",
    repository: String = "rocket",
    number: Long = 42,
    title: String = "Add thrusters",
    createdAt: String = "2026-01-01T09:00:00Z",
    timeline: List<String>,
) = """
    {
      "number": $number,
      "title": "$title",
      "url": "https://github.com/$owner/$repository/pull/$number",
      "createdAt": "$createdAt",
      "repository": { "name": "$repository", "owner": { "login": "$owner" } },
      "timelineItems": { "nodes": [${timeline.joinToString()}] }
    }
"""

fun reviewRequested(at: String, reviewer: String) =
    """{ "__typename": "ReviewRequestedEvent", "createdAt": "$at", "requestedReviewer": $reviewer }"""

fun reviewRequestRemoved(at: String, reviewer: String) =
    """{ "__typename": "ReviewRequestRemovedEvent", "createdAt": "$at", "requestedReviewer": $reviewer }"""

fun submittedReview(at: String, author: String) =
    """{ "__typename": "PullRequestReview", "state": "APPROVED", "submittedAt": "$at", "author": { "login": "$author" } }"""

fun draftReview(author: String) =
    """{ "__typename": "PullRequestReview", "state": "PENDING", "submittedAt": null, "author": { "login": "$author" } }"""

fun user(login: String) = """{ "__typename": "User", "login": "$login" }"""

fun team(combinedSlug: String) = """{ "__typename": "Team", "combinedSlug": "$combinedSlug" }"""

fun mannequin() = """{ "__typename": "Mannequin" }"""

const val DELETED_REVIEWER = "null"
