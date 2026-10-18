package com.jimbroze.reviewqueue.settings

import com.jimbroze.reviewqueue.queue.SortOrder
import com.jimbroze.reviewqueue.queue.SortRule.DirectRequestsFirst
import com.jimbroze.reviewqueue.queue.SortRule.DraftsLast
import com.jimbroze.reviewqueue.queue.SortRule.LongestWaitingFirst
import com.jimbroze.reviewqueue.queue.SortRule.ReReviewsFirst
import org.junit.Assert.assertEquals
import org.junit.Test

class SortOrderSettingTest {

    @Test
    fun `stored rule names are restored in their stored order`() {
        assertEquals(listOf(LongestWaitingFirst, DraftsLast), SortOrderSetting.restore("LongestWaitingFirst,DraftsLast").rules)
    }

    @Test
    fun `a stored order survives being stored`() {
        val order = SortOrder.of(listOf(DirectRequestsFirst, ReReviewsFirst))

        assertEquals(order, SortOrderSetting.restore(SortOrderSetting.store(order)))
    }

    @Test
    fun `stored names that are not rules are left out`() {
        assertEquals(listOf(ReReviewsFirst), SortOrderSetting.restore("NewestFirst,ReReviewsFirst").rules)
    }

    @Test
    fun `nothing stored means the default order`() {
        assertEquals(SortOrder.Default, SortOrderSetting.restore(null))
    }

    @Test
    fun `an empty stored order stays empty`() {
        assertEquals(emptyList<Any>(), SortOrderSetting.restore("").rules)
    }

    @Test
    fun `for editing, the chosen rules come first and in order, then the unchosen ones`() {
        val choices = SortOrderSetting.choicesFor(SortOrder.of(listOf(LongestWaitingFirst, DraftsLast)))

        assertEquals(
            listOf(
                SortRuleChoice(LongestWaitingFirst, isChosen = true),
                SortRuleChoice(DraftsLast, isChosen = true),
                SortRuleChoice(ReReviewsFirst, isChosen = false),
                SortRuleChoice(DirectRequestsFirst, isChosen = false),
            ),
            choices,
        )
    }

    @Test
    fun `edited choices give the chosen rules in their listed order`() {
        val choices = listOf(
            SortRuleChoice(DirectRequestsFirst, isChosen = true),
            SortRuleChoice(DraftsLast, isChosen = false),
            SortRuleChoice(ReReviewsFirst, isChosen = true),
            SortRuleChoice(LongestWaitingFirst, isChosen = false),
        )

        assertEquals(listOf(DirectRequestsFirst, ReReviewsFirst), SortOrderSetting.orderOf(choices).rules)
    }
}
