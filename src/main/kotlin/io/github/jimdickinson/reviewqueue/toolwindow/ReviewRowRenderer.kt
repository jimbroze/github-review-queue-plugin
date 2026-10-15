package io.github.jimdickinson.reviewqueue.toolwindow

import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class ReviewRowRenderer : ListCellRenderer<ReviewRow> {
    private val summaryLine = SimpleColoredComponent().apply { isOpaque = false; ipad = JBUI.emptyInsets() }
    private val titleLine = JBLabel()
    private val cell = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(4, 6)
        add(summaryLine, BorderLayout.NORTH)
        add(titleLine, BorderLayout.CENTER)
    }

    override fun getListCellRendererComponent(
        list: JList<out ReviewRow>,
        row: ReviewRow,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ): Component {
        showSummary(row)
        titleLine.text = row.title
        cell.background = UIUtil.getListBackground(selected, list.hasFocus())
        titleLine.foreground = UIUtil.getListForeground(selected, list.hasFocus())
        return cell
    }

    private fun showSummary(row: ReviewRow) {
        summaryLine.clear()
        if (row.isReReview) summaryLine.append("${ReviewQueueBundle.message("row.reReview")}  ", reReviewBadge)
        summaryLine.append("${row.pullRequestLabel}  ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        summaryLine.append(
            ReviewQueueBundle.message("row.details", row.waitingFor, row.requestedFrom),
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
