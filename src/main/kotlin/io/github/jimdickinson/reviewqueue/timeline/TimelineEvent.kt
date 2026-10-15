package io.github.jimdickinson.reviewqueue.timeline

import java.time.Instant

sealed interface Reviewer {
    data class User(val login: String) : Reviewer
    data class Team(val slug: String) : Reviewer
}

sealed interface TimelineEvent {
    val at: Instant

    data class ReviewRequested(override val at: Instant, val reviewer: Reviewer) : TimelineEvent
    data class ReviewRequestRemoved(override val at: Instant, val reviewer: Reviewer) : TimelineEvent
    data class ReviewSubmitted(override val at: Instant, val authorLogin: String) : TimelineEvent
}
