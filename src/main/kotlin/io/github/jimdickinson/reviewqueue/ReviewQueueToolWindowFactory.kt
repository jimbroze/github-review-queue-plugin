package io.github.jimdickinson.reviewqueue

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.ContentFactory

class ReviewQueueToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val placeholder = JBLabel("Review Queue is not built yet.")
        val content = ContentFactory.getInstance().createContent(placeholder, null, false)
        toolWindow.contentManager.addContent(content)
    }
}
