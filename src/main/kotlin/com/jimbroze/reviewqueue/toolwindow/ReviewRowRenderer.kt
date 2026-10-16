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
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

/** Draws a review row, wrapping its title and details onto more lines when the list is too narrow for them. */
class ReviewRowRenderer : ListCellRenderer<ReviewRow> {
    private val reReviewPill = JBLabel(ReviewQueueBundle.message("row.reReview")).apply {
        font = JBFont.small().asBold()
        foreground = reReviewColour
        border = BorderFactory.createCompoundBorder(
            RoundedLineBorder(reReviewColour, JBUI.scale(12)),
            JBUI.Borders.empty(0, 6),
        )
    }
    private val detailMeasurer = SimpleColoredComponent().apply { ipad = JBUI.emptyInsets() }
    private val cell = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(4, 6)
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
        val textWidth = textWidthIn(list)
        cell.removeAll()
        addTitleLines(row, foreground, textWidth)
        addDetailLines(row, foreground, if (isHighlightedForSelection) foreground else urgencyColour(row.urgency), textWidth)
        cell.background = UIUtil.getListBackground(selected, list.hasFocus())
        return cell
    }

    private fun textWidthIn(list: JList<*>): Int {
        if (list.width <= 0) return Int.MAX_VALUE
        return list.width - cell.insets.left - cell.insets.right - textIndent()
    }

    private fun addTitleLines(row: ReviewRow, foreground: Color, textWidth: Int) {
        val font = if (row.isDraft) JBFont.label() else JBFont.label().asBold()
        val words = row.title.split(' ').filter(String::isNotEmpty)
        val metrics = cell.getFontMetrics(font)
        val lines = wrapIntoLines(words, textWidth, metrics.stringWidth(" "), TITLE_MAX_LINES, metrics::stringWidth)
            .map { it.joinToString(" ") }
            .ifEmpty { listOf("") }
        val titleLabels = lines.mapIndexed { index, text ->
            ShrinksOnlyHorizontallyLabel().apply {
                this.text = text
                this.font = font
                this.foreground = if (row.isDraft) UIUtil.getContextHelpForeground() else foreground
                if (index == 0) showIcon(if (row.isDraft) GithubIcons.PullRequestDraft else CollaborationToolsIcons.PullRequestOpen)
                else border = JBUI.Borders.emptyLeft(ICON_WIDTH + ICON_GAP)
            }
        }
        titleLabels.dropLast(1).forEach { cell.add(lineOf(it)) }
        addLastTitleLine(titleLabels.last(), row.isReReview, textWidth)
    }

    private fun addLastTitleLine(titleLabel: JBLabel, isReReview: Boolean, textWidth: Int) {
        val pillFitsBesideTitle = titleLabel.preferredSize.width - textIndent() + pillWidthWithGap() <= textWidth
        when {
            !isReReview -> cell.add(lineOf(titleLabel))
            pillFitsBesideTitle -> cell.add(lineOf(titleLabel, Box.createHorizontalStrut(JBUI.scale(ICON_GAP)), reReviewPill))
            else -> {
                cell.add(lineOf(titleLabel))
                cell.add(lineOf(Box.createHorizontalStrut(textIndent()), reReviewPill))
            }
        }
    }

    private fun pillWidthWithGap() = JBUI.scale(ICON_GAP) + reReviewPill.preferredSize.width

    private fun JBLabel.showIcon(icon: Icon) {
        this.icon = icon
        iconTextGap = JBUI.scale(ICON_GAP)
    }

    private fun addDetailLines(row: ReviewRow, foreground: Color, waitingTimeColour: Color?, textWidth: Int) {
        val details = detailsOf(row, waitingTimeColour)
        wrapIntoLines(details, textWidth, widthOf(SEPARATOR, detailStyle), Int.MAX_VALUE) { widthOf(it.text, it.style) }
            .forEach { cell.add(detailLine(it, foreground)) }
    }

    private fun detailsOf(row: ReviewRow, waitingTimeColour: Color?): List<Detail> = buildList {
        add(Detail(row.waitingFor, waitingTimeStyle(waitingTimeColour)))
        add(Detail(row.pullRequestLabel, detailStyle))
        add(Detail(ReviewQueueBundle.message("row.author", row.authorLogin), detailStyle))
        add(Detail(ReviewQueueBundle.message("row.requestedFrom", row.requestedFrom), detailStyle))
        if (row.isDraft) add(Detail(ReviewQueueBundle.message("row.draft"), detailStyle))
    }

    private fun detailLine(details: List<Detail>, foreground: Color) = SimpleColoredComponent().apply {
        isOpaque = false
        alignmentX = Component.LEFT_ALIGNMENT
        ipad = JBUI.insetsLeft(ICON_WIDTH + ICON_GAP)
        this.foreground = foreground
        details.forEachIndexed { index, detail ->
            if (index > 0) append(SEPARATOR, detailStyle)
            append(detail.text, detail.style)
        }
    }

    private fun widthOf(text: String, style: SimpleTextAttributes): Int {
        detailMeasurer.clear()
        detailMeasurer.append(text, style)
        return detailMeasurer.preferredSize.width
    }

    private fun lineOf(vararg parts: Component) = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false
        alignmentX = Component.LEFT_ALIGNMENT
        parts.forEach(::add)
        add(Box.createHorizontalGlue())
    }

    private fun textIndent() = JBUI.scale(ICON_WIDTH + ICON_GAP)

    private fun urgencyColour(urgency: Urgency): Color? = when (urgency) {
        Urgency.OnTime -> null
        Urgency.NearlyOverdue -> nearlyOverdueColour
        Urgency.Overdue -> overdueColour
    }

    private fun waitingTimeStyle(colour: Color?) =
        SimpleTextAttributes(SimpleTextAttributes.STYLE_SMALLER or SimpleTextAttributes.STYLE_BOLD, colour)

    private data class Detail(val text: String, val style: SimpleTextAttributes)

    private class ShrinksOnlyHorizontallyLabel : JBLabel() {
        override fun getMinimumSize() = Dimension(0, preferredSize.height)
    }

    private companion object {
        const val ICON_WIDTH = 16
        const val ICON_GAP = 6
        const val TITLE_MAX_LINES = 2
        const val SEPARATOR = " · "
        val detailStyle: SimpleTextAttributes = SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES
        val reReviewColour = JBColor.namedColor("Label.warningForeground", JBColor(0xA35200, 0xE5A50A))
        val nearlyOverdueColour = reReviewColour
        val overdueColour = JBColor.namedColor("Label.errorForeground", JBColor(0xC7222D, 0xFF5261))
    }
}
