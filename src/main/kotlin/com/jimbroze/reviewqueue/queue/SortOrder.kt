package com.jimbroze.reviewqueue.queue

enum class SortRule(internal val comparator: Comparator<ReviewRequest>) {
    DraftsLast(compareBy { it.isDraft }),
    ReReviewsFirst(compareByDescending { it.isReReview }),
    LongestWaitingFirst(compareBy { it.requestedAt }),
    DirectRequestsFirst(compareByDescending { it.requestedReviewer == RequestedReviewer.You }),
}

/** The rules that order the queue, most important first; each rule appears at most once. */
@JvmInline
value class SortOrder private constructor(val rules: List<SortRule>) {
    internal val comparator: Comparator<ReviewRequest>
        get() = rules.map(SortRule::comparator).fold(Comparator { _, _ -> 0 }, Comparator<ReviewRequest>::then)

    companion object {
        val Default = SortOrder(
            listOf(SortRule.DraftsLast, SortRule.ReReviewsFirst, SortRule.LongestWaitingFirst, SortRule.DirectRequestsFirst),
        )

        /** Returns the order of [rules]; a rule given more than once keeps only its first position. */
        fun of(rules: List<SortRule>): SortOrder = SortOrder(rules.distinct())
    }
}
