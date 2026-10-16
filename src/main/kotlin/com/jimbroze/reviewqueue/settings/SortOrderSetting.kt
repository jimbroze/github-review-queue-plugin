package com.jimbroze.reviewqueue.settings

import com.jimbroze.reviewqueue.queue.SortOrder
import com.jimbroze.reviewqueue.queue.SortRule

/** A sort rule as listed in settings, and whether the user has chosen to sort by it. */
data class SortRuleChoice(val rule: SortRule, val isChosen: Boolean)

/** Converts the queue's [SortOrder] to and from its stored and editable forms. */
object SortOrderSetting {
    private const val SEPARATOR = ","

    /** Returns the order stored as [stored]; nothing stored is the default order, and unknown rule names are left out. */
    fun restore(stored: String?): SortOrder =
        if (stored == null) {
            SortOrder.Default
        } else {
            SortOrder.of(stored.split(SEPARATOR).mapNotNull { name -> SortRule.entries.firstOrNull { it.name == name } })
        }

    fun store(order: SortOrder): String = order.rules.joinToString(SEPARATOR) { it.name }

    /** Lists every rule: those in [order] first, chosen and in order, then the rest, unchosen. */
    fun choicesFor(order: SortOrder): List<SortRuleChoice> =
        order.rules.map { SortRuleChoice(it, isChosen = true) } +
            (SortRule.entries - order.rules.toSet()).map { SortRuleChoice(it, isChosen = false) }

    fun orderOf(choices: List<SortRuleChoice>): SortOrder = SortOrder.of(choices.filter { it.isChosen }.map { it.rule })
}
