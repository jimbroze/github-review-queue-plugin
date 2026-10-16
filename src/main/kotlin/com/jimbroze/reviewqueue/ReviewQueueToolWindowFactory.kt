package com.jimbroze.reviewqueue

import com.intellij.openapi.components.service
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.jimbroze.reviewqueue.toolwindow.ReviewQueuePanel
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueService

class ReviewQueueToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val reviewQueue = project.service<ReviewQueueService>()
        val panel = ReviewQueuePanel(
            parentDisposable = toolWindow.disposable,
            onRefresh = reviewQueue::refresh,
            onOpen = reviewQueue::open,
            onOpenGitHubSettings = { openGitHubSettings(project) },
        )
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, null, false))
        reviewQueue.renderStateOnEdt(until = toolWindow.disposable, render = panel::render)
        reviewQueue.refresh()
    }

    private fun openGitHubSettings(project: Project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            { (it as? SearchableConfigurable)?.id == GITHUB_SETTINGS_ID },
            null,
        )
    }

    private companion object {
        const val GITHUB_SETTINGS_ID = "org.jetbrains.plugins.github.ui.GithubSettingsConfigurable"
    }
}
