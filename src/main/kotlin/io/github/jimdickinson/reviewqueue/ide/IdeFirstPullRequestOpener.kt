package io.github.jimdickinson.reviewqueue.ide

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.toolwindow.PullRequestOpener

class IdeFirstPullRequestOpener<P>(
    private val currentProject: P,
    private val openProjects: () -> List<P>,
    private val viewer: PullRequestViewer<P>,
    private val bringToFront: (P) -> Unit,
    private val browser: PullRequestOpener,
) {
    suspend fun open(pullRequest: PullRequestRef, url: String) {
        TODO()
    }
}
