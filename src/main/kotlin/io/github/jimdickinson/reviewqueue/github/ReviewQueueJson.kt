package io.github.jimdickinson.reviewqueue.github

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import io.github.jimdickinson.reviewqueue.timeline.Reviewer
import io.github.jimdickinson.reviewqueue.timeline.TimelineEvent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.time.Instant

internal data class ReviewQueueData(val teamSlugs: Set<String>, val pullRequests: List<SearchedPullRequest>)

internal data class SearchedPullRequest(
    val ref: PullRequestRef,
    val title: String,
    val authorLogin: String,
    val isDraft: Boolean,
    val url: String,
    val createdAt: Instant,
    val timeline: List<TimelineEvent>,
)

private const val DRAFT_REVIEW_STATE = "PENDING"

private const val DELETED_USER_LOGIN = "ghost"

internal fun parseViewerLogin(data: JsonObject): String = data.obj("viewer").string("login")

internal fun parseReviewQueue(data: JsonObject): ReviewQueueData = ReviewQueueData(
    teamSlugs = viewerTeamSlugs(data.obj("viewer")),
    pullRequests = data.obj("search").nodes().map(::searchedPullRequest),
)

private fun viewerTeamSlugs(viewer: JsonObject): Set<String> =
    viewer.obj("organizations").nodes()
        .flatMap { organization -> organization.obj("teams").nodes() }
        .map { team -> team.string("combinedSlug") }
        .toSet()

private fun searchedPullRequest(pullRequest: JsonObject): SearchedPullRequest {
    val repository = pullRequest.obj("repository")
    return SearchedPullRequest(
        ref = PullRequestRef(
            RepositoryName(owner = repository.obj("owner").string("login"), name = repository.string("name")),
            pullRequest.getValue("number").jsonPrimitive.long,
        ),
        title = pullRequest.string("title"),
        authorLogin = authorLogin(pullRequest),
        isDraft = pullRequest.getValue("isDraft").jsonPrimitive.boolean,
        url = pullRequest.string("url"),
        createdAt = pullRequest.instant("createdAt"),
        timeline = pullRequest.obj("timelineItems").nodes().mapNotNull(::timelineEvent),
    )
}

private fun authorLogin(pullRequest: JsonObject): String =
    (pullRequest["author"] as? JsonObject)?.string("login") ?: DELETED_USER_LOGIN

private fun timelineEvent(item: JsonObject): TimelineEvent? = when (item.string("__typename")) {
    "ReviewRequestedEvent" -> requestedReviewer(item)?.let { TimelineEvent.ReviewRequested(item.instant("createdAt"), it) }
    "ReviewRequestRemovedEvent" -> requestedReviewer(item)?.let { TimelineEvent.ReviewRequestRemoved(item.instant("createdAt"), it) }
    "PullRequestReview" -> submittedReview(item)
    else -> null
}

private fun requestedReviewer(event: JsonObject): Reviewer? {
    val reviewer = event["requestedReviewer"] as? JsonObject ?: return null
    return when (reviewer.string("__typename")) {
        "User" -> Reviewer.User(reviewer.string("login"))
        "Team" -> Reviewer.Team(reviewer.string("combinedSlug"))
        else -> null
    }
}

private fun submittedReview(review: JsonObject): TimelineEvent.ReviewSubmitted? {
    if (review.string("state") == DRAFT_REVIEW_STATE) return null
    val authorLogin = (review["author"] as? JsonObject)?.string("login") ?: return null
    return TimelineEvent.ReviewSubmitted(review.instant("submittedAt"), authorLogin)
}

private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.instant(key: String): Instant = Instant.parse(string(key))

// GitHub returns a null node for anything the viewer may not see, e.g. an organisation behind SAML sign-in.
private fun JsonObject.nodes(): List<JsonObject> = getValue("nodes").jsonArray.filterIsInstance<JsonObject>()
