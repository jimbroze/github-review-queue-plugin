package com.jimbroze.reviewqueue.queue

import org.junit.Assert.assertEquals
import org.junit.Test

class SortOrderTest {

    @Test
    fun `the default order puts drafts last, then re-reviews, then longest waiting, then direct requests`() {
        assertEquals(
            listOf(SortRule.DraftsLast, SortRule.ReReviewsFirst, SortRule.LongestWaitingFirst, SortRule.DirectRequestsFirst),
            SortOrder.Default.rules,
        )
    }

    @Test
    fun `rules keep the order they are given in`() {
        val order = SortOrder.of(listOf(SortRule.LongestWaitingFirst, SortRule.DraftsLast))

        assertEquals(listOf(SortRule.LongestWaitingFirst, SortRule.DraftsLast), order.rules)
    }

    @Test
    fun `a rule given twice counts once, at its first position`() {
        val order = SortOrder.of(listOf(SortRule.ReReviewsFirst, SortRule.DraftsLast, SortRule.ReReviewsFirst))

        assertEquals(listOf(SortRule.ReReviewsFirst, SortRule.DraftsLast), order.rules)
    }

    @Test
    fun `sort orders with the same rules in the same order are equal`() {
        assertEquals(SortOrder.of(listOf(SortRule.DraftsLast)), SortOrder.of(listOf(SortRule.DraftsLast)))
    }
}
