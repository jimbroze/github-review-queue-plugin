package com.jimbroze.reviewqueue.settings

import com.intellij.ui.CheckBoxList
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Panel
import com.jimbroze.reviewqueue.queue.SortOrder
import com.jimbroze.reviewqueue.queue.SortRule
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueBundle

/** The settings rows for choosing and ranking the rules that order the queue, saved to [settings]. */
class SortSection(private val settings: ReviewQueueSettings) {
    private val rulesList = CheckBoxList<SortRule>()

    fun addTo(panel: Panel) = with(panel) {
        show(settings.sortOrder)
        group(ReviewQueueBundle.message("settings.sortOrder")) {
            row {
                cell(ToolbarDecorator.createDecorator(rulesList).disableAddAction().disableRemoveAction().createPanel())
                    .comment(ReviewQueueBundle.message("settings.sortOrder.comment"))
                    .onReset { show(settings.sortOrder) }
                    .onIsModified { editedOrder() != settings.sortOrder }
                    .onApply { settings.sortOrder = editedOrder() }
            }
        }
    }

    private fun show(order: SortOrder) {
        rulesList.clear()
        SortOrderSetting.choicesFor(order).forEach { rulesList.addItem(it.rule, labelOf(it.rule), it.isChosen) }
    }

    private fun editedOrder(): SortOrder =
        SortOrderSetting.orderOf(
            (0 until rulesList.itemsCount).mapNotNull(rulesList::getItemAt).map { SortRuleChoice(it, rulesList.isItemSelected(it)) },
        )

    private fun labelOf(rule: SortRule): String = ReviewQueueBundle.message("settings.sortRule.${rule.name}")
}
