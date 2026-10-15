package io.github.jimdickinson.reviewqueue.ide

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import io.github.jimdickinson.reviewqueue.toolwindow.PullRequestOpener
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class IdeFirstPullRequestOpener<P>(
    private val currentProject: P,
    private val openProjects: () -> List<P>,
    private val viewer: PullRequestViewer<P>,
    private val bringToFront: (P) -> Unit,
    private val browser: PullRequestOpener,
) {
    suspend fun open(pullRequest: PullRequestRef, url: String) {
        val project = preferredProjectWith(pullRequest.repository)
        if (project == null || !showIn(project, pullRequest)) {
            browser.open(pullRequest, url)
        }
    }

    private suspend fun preferredProjectWith(repository: RepositoryName): P? = coroutineScope {
        val candidates = listOf(currentProject) + openProjects().filter { it != currentProject }
        val answers = candidates.map { async { viewer.hasRepository(it, repository) } }
        val preferred = candidates.zip(answers).firstOrNull { (_, hasRepository) -> hasRepository.await() }?.first
        answers.forEach { it.cancel() }
        preferred
    }

    private suspend fun showIn(project: P, pullRequest: PullRequestRef): Boolean {
        if (project != currentProject) bringToFront(project)
        return viewer.show(project, pullRequest)
    }
}
