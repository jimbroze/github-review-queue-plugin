package io.github.jimdickinson.reviewqueue.github

import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal sealed interface GraphQlAnswer<out T> {
    data class Answered<T>(val value: T) : GraphQlAnswer<T>
    data class Refused(val failure: ReviewRequestsResult.Failure) : GraphQlAnswer<Nothing>
}

internal inline fun <T> GraphQlAnswer<T>.valueOr(onRefused: (ReviewRequestsResult.Failure) -> Nothing): T = when (this) {
    is GraphQlAnswer.Answered -> value
    is GraphQlAnswer.Refused -> onRefused(failure)
}

/** Hands the response's `data` to [parse], or says why GitHub gave none; never throws. */
internal fun <T> GraphQlResponse.answer(parse: (data: JsonObject) -> T): GraphQlAnswer<T> = when (this) {
    is GraphQlResponse.Body -> readBody(json, parse)
    GraphQlResponse.Unauthorized -> GraphQlAnswer.Refused(ReviewRequestsResult.Unauthorized)
    is GraphQlResponse.RateLimited -> GraphQlAnswer.Refused(ReviewRequestsResult.RateLimited(resetAt))
    is GraphQlResponse.Failed -> GraphQlAnswer.Refused(ReviewRequestsResult.Failed(reason))
}

private fun <T> readBody(json: String, parse: (JsonObject) -> T): GraphQlAnswer<T> = try {
    answer(Json.parseToJsonElement(json).jsonObject, parse)
} catch (unexpectedShape: Exception) {
    GraphQlAnswer.Refused(ReviewRequestsResult.Failed("Unexpected answer from GitHub: ${unexpectedShape.message}"))
}

// GitHub can return partial `data` alongside `errors`, e.g. nulls for organisations behind SAML sign-in;
// the data that did come back is still valid.
private fun <T> answer(body: JsonObject, parse: (JsonObject) -> T): GraphQlAnswer<T> {
    val errors = errors(body)
    val data = body["data"] as? JsonObject
    return when {
        errors.any(::isRateLimit) -> GraphQlAnswer.Refused(ReviewRequestsResult.RateLimited(resetAt = null))
        data != null -> GraphQlAnswer.Answered(parse(data))
        else -> GraphQlAnswer.Refused(ReviewRequestsResult.Failed(failureReason(errors)))
    }
}

private fun errors(body: JsonObject): List<JsonObject> =
    (body["errors"] as? JsonArray).orEmpty().map { it.jsonObject }

private fun isRateLimit(error: JsonObject) = error["type"]?.jsonPrimitive?.content == "RATE_LIMITED"

private fun failureReason(errors: List<JsonObject>) =
    errors.joinToString("; ", transform = ::message).ifEmpty { "GitHub answered without data" }

private fun message(error: JsonObject) = error.getValue("message").jsonPrimitive.content
