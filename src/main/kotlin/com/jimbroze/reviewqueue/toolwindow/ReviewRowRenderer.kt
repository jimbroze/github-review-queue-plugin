package com.jimbroze.reviewqueue.toolwindow

import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jimbroze.reviewqueue.worktime.Urgency
import icons.CollaborationToolsIcons
import org.jetbrains.plugins.github.GithubIcons
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class ReviewRowRenderer : ListCellRenderer<ReviewRow> {
    private val titleLabel = ShrinksOnlyHorizontallyLabel().apply {
        iconTextGap = JBUI.scale(ICON_GAP)
    }
    private val reReviewPill = JBLabel(ReviewQueueBundle.message("row.reReview")).apply {
        font = JBFont.small().asBold()
        foreground = reReviewColour
        border = BorderFactory.createCompoundBorder(
            RoundedLineBorder(reReviewColour, JBUI.scale(12)),
            JBUI.Borders.empty(0, 6),
        )
    }
    private val titleLine = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false
        add(titleLabel)
        add(Box.createHorizontalStrut(JBUI.scale(ICON_GAP)))
        add(reReviewPill)
        add(Box.createHorizontalGlue())
    }
    private val detailsLine = SimpleColoredComponent().apply {
        isOpaque = false
        ipad = JBUI.insetsLeft(ICON_WIDTH + ICON_GAP)
    }
    private val cell = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(4, 6)
        add(titleLine, BorderLayout.NORTH)
        add(detailsLine, BorderLayout.CENTER)
    }

    override fun getListCellRendererComponent(
        list: JList<out ReviewRow>,
        row: ReviewRow,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ): Component {
        val foreground = UIUtil.getListForeground(selected, list.hasFocus())
        val isHighlightedForSelection = selected && list.hasFocus()
        showTitle(row, foreground)
        showDetails(row, foreground, if (isHighlightedForSelection) foreground else urgencyColour(row.urgency))
        cell.background = UIUtil.getListBackground(selected, list.hasFocus())
        return cell
    }

    private fun showTitle(row: ReviewRow, foreground: Color) {
        titleLabel.text = row.title
        titleLabel.icon = if (row.isDraft) GithubIcons.PullRequestDraft else CollaborationToolsIcons.PullRequestOpen
        titleLabel.font = if (row.isDraft) JBFont.label() else JBFont.label().asBold()
        titleLabel.foreground = if (row.isDraft) UIUtil.getContextHelpForeground() else foreground
        reReviewPill.isVisible = row.isReReview
    }

    private fun showDetails(row: ReviewRow, foreground: Color, waitingTimeColour: Color?) {
        detailsLine.clear()
        detailsLine.foreground = foreground
        detailsLine.append(ReviewQueueBundle.message("row.waiting", row.waitingFor), waitingTimeStyle(waitingTimeColour))
        appendDetail(ReviewQueueBundle.message("row.details", row.pullRequestLabel, row.authorLogin, row.requestedFrom))
        if (row.isDraft) appendDetail(ReviewQueueBundle.message("row.draft"))
    }

    private fun urgencyColour(urgency: Urgency): Color? = when (urgency) {
        Urgency.OnTime -> null
        Urgency.NearlyOverdue -> nearlyOverdueColour
        Urgency.Overdue -> overdueColour
    }

    private fun waitingTimeStyle(colour: Color?) =
        SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER or SimpleTextAttributes.STYLE_BOLD, colour)

    private fun appendDetail(text: String) {
        detailsLine.append(SEPARATOR + text, SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
    }

    private class ShrinksOnlyHorizontallyLabel : JBLabel() {
        override fun getMinimumSize() = Dimension(0, preferredSize.height)
    }

    private companion object {
        const val ICON_WIDTH = 16
        const val ICON_GAP = 6
        const val SEPARATOR = " · "
        val reReviewColour = JBColor.namedColor("Label.warningForeground", JBColor(0xA35200, 0xE5A50A))
        val nearlyOverdueColour = reReviewColour
        val overdueColour = JBColor.namedColor("Label.errorForeground", JBColor(0xC7222D, 0xFF5261))
    }
}
