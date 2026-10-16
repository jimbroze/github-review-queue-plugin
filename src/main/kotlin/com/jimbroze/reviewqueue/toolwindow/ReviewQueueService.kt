package com.jimbroze.reviewqueue.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jimbroze.reviewqueue.ide.ideFirstPullRequestOpener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Clock

@Service(Service.Level.PROJECT)
class ReviewQueueService(project: Project, private val scope: CoroutineScope) {
    private val ideFirstOpener = ideFirstPullRequestOpener(project)

    private val viewModel = ReviewQueueViewModel(
        source = ideAccountReviewRequests,
        opener = { pullRequest, url -> scope.launch { ideFirstOpener.open(pullRequest, url) } },
        clock = Clock.systemUTC(),
    )

    fun refresh() {
        scope.launch { viewModel.refresh() }
    }

    fun open(row: ReviewRow) {
        viewModel.open(row)
    }

    fun renderStateOnEdt(until: Disposable, render: (ReviewQueueState) -> Unit) {
        val rendering = scope.launch(Dispatchers.EDT) { viewModel.state.collect(render) }
        Disposer.register(until) { rendering.cancel() }
    }
}
