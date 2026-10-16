package com.jimbroze.reviewqueue.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.jimbroze.reviewqueue.ide.ideFirstPullRequestOpener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** This project's view of the [SharedReviewQueue]: opens rows from this project and watches while its tool window shows. */
@Service(Service.Level.PROJECT)
class ReviewQueueService(private val project: Project, private val scope: CoroutineScope) {
    private val sharedQueue = service<SharedReviewQueue>()
    private val ideFirstOpener = ideFirstPullRequestOpener(project)
    private val opener = PullRequestOpener { pullRequest, url -> scope.launch { ideFirstOpener.open(pullRequest, url) } }

    fun refresh() {
        sharedQueue.refresh()
    }

    fun open(row: ReviewRow) {
        opener.open(row)
    }

    fun renderStateOnEdt(until: Disposable, render: (ReviewQueueState) -> Unit) {
        val rendering = scope.launch(Dispatchers.EDT) { sharedQueue.state.collect(render) }
        Disposer.register(until) { rendering.cancel() }
    }

    /** Counts [toolWindow] as watching the queue whenever it is visible. */
    fun watchWhileVisible(toolWindow: ToolWindow) {
        val watching = WatchWhileVisible(toolWindow)
        project.messageBus.connect(toolWindow.disposable).subscribe(
            ToolWindowManagerListener.TOPIC,
            object : ToolWindowManagerListener {
                override fun stateChanged(toolWindowManager: ToolWindowManager) = watching.update()
            },
        )
        Disposer.register(toolWindow.disposable, watching)
    }

    private inner class WatchWhileVisible(private val toolWindow: ToolWindow) : Disposable {
        private var watch: QueueWatchers.Watch? = null

        init {
            update()
        }

        fun update() {
            if (toolWindow.isVisible) startWatching() else stopWatching()
        }

        override fun dispose() = stopWatching()

        private fun startWatching() {
            if (watch == null) watch = sharedQueue.watch()
        }

        private fun stopWatching() {
            watch?.close()
            watch = null
        }
    }
}
