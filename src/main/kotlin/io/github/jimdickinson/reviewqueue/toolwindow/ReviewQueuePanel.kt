package io.github.jimdickinson.reviewqueue.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.CollectionListModel
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.MouseEvent

class ReviewQueuePanel(
    parentDisposable: Disposable,
    private val onRefresh: () -> Unit,
    private val onOpen: (ReviewRow) -> Unit,
    private val onOpenGitHubSettings: () -> Unit,
) : SimpleToolWindowPanel(true, true) {
    private val rows = CollectionListModel<ReviewRow>()
    private val list = JBList(rows).apply {
        cellRenderer = ReviewRowRenderer()
        fitRowsToVisibleWidth()
    }
    private val loadingPanel = JBLoadingPanel(BorderLayout(), parentDisposable)
    private var isFetching = false

    init {
        loadingPanel.add(ScrollPaneFactory.createScrollPane(list, true))
        setContent(loadingPanel)
        setToolbar(createToolbar())
        openRowOnDoubleClick()
        openSelectedRowOnEnter()
    }

    fun render(state: ReviewQueueState) {
        isFetching = state.isFetching
        rows.replaceAll((state as? ReviewQueueState.Showing)?.rows.orEmpty())
        showStatus(state)
        if (isFetching) loadingPanel.startLoading() else loadingPanel.stopLoading()
    }

    private fun showStatus(state: ReviewQueueState) {
        val status = list.emptyText.clear()
        when (state) {
            ReviewQueueState.Loading, is ReviewQueueState.Showing -> Unit
            ReviewQueueState.NothingWaiting -> status.appendLine(ReviewQueueBundle.message("status.nothingWaiting"))
            ReviewQueueState.NeedsSignIn -> askToSignIn(ReviewQueueBundle.message("status.needsSignIn"))
            ReviewQueueState.SignInExpired -> askToSignIn(ReviewQueueBundle.message("status.signInExpired"))
            is ReviewQueueState.RateLimited -> status.appendLine(rateLimitMessage(state.resetsIn))
            is ReviewQueueState.CouldNotLoad -> {
                status.appendLine(ReviewQueueBundle.message("status.couldNotLoad"))
                status.appendLine(state.reason)
                status.appendLine(ReviewQueueBundle.message("status.retry"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) { onRefresh() }
            }
        }
    }

    private fun askToSignIn(reason: String) {
        list.emptyText.appendLine(reason)
        list.emptyText.appendLine(ReviewQueueBundle.message("status.openGitHubSettings"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
            onOpenGitHubSettings()
        }
    }

    private fun rateLimitMessage(resetsIn: String?): String =
        if (resetsIn == null) ReviewQueueBundle.message("status.rateLimited") else ReviewQueueBundle.message("status.rateLimited.resetsIn", resetsIn)

    private fun createToolbar() =
        ActionManager.getInstance()
            .createActionToolbar(TOOLBAR_PLACE, DefaultActionGroup(RefreshAction()), true)
            .apply { targetComponent = this@ReviewQueuePanel }
            .component

    private fun openRowOnDoubleClick() {
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                val row = rowAt(event.point) ?: return false
                onOpen(row)
                return true
            }
        }.installOn(list)
    }

    private fun JBList<ReviewRow>.fitRowsToVisibleWidth() {
        fixedCellWidth = 1
    }

    private fun rowAt(point: Point): ReviewRow? {
        val index = list.locationToIndex(point)
        val isOnRow = index >= 0 && list.getCellBounds(index, index)?.contains(point) == true
        return if (isOnRow) rows.getElementAt(index) else null
    }

    private fun openSelectedRowOnEnter() {
        DumbAwareAction.create { list.selectedValue?.let(onOpen) }
            .registerCustomShortcutSet(CommonShortcuts.ENTER, list)
    }

    private inner class RefreshAction : DumbAwareAction(
        ReviewQueueBundle.message("action.refresh.text"),
        ReviewQueueBundle.message("action.refresh.description"),
        AllIcons.Actions.Refresh,
    ) {
        override fun actionPerformed(event: AnActionEvent) = onRefresh()

        override fun update(event: AnActionEvent) {
            event.presentation.isEnabled = !isFetching
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private companion object {
        const val TOOLBAR_PLACE = "ReviewQueueToolbar"
    }
}
