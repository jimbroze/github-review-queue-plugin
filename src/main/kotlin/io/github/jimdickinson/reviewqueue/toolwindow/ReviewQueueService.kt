package io.github.jimdickinson.reviewqueue.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Clock

@Service(Service.Level.PROJECT)
class ReviewQueueService(private val scope: CoroutineScope) {
    private val viewModel = ReviewQueueViewModel(
        source = ideAccountReviewRequests,
        opener = BrowserPullRequestOpener,
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
