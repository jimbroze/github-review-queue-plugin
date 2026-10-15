package io.github.jimdickinson.reviewqueue.toolwindow

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JList

class ReviewRowRenderer : ColoredListCellRenderer<ReviewRow>() {
    override fun customizeCellRenderer(
        list: JList<out ReviewRow>,
        row: ReviewRow,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ) {
        if (row.isReReview) append("${ReviewQueueBundle.message("row.reReview")}  ", reReviewBadge)
        append("${row.pullRequestLabel}  ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        append(row.title)
        append(
            "  ${ReviewQueueBundle.message("row.details", row.waitingFor, row.requestedFrom)}",
            SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES,
        )
    }

    private companion object {
        val reReviewBadge = SimpleTextAttributes(
            SimpleTextAttributes.STYLE_BOLD,
            JBColor.namedColor("Label.warningForeground", JBColor(0xA35200, 0xE5A50A)),
        )
    }
}
