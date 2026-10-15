package io.github.jimdickinson.reviewqueue.ide

import com.intellij.collaboration.util.URIUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.pullrequest.ui.GHPRProjectViewModel
import org.jetbrains.plugins.github.util.GHGitRepositoryMapping
import kotlin.time.Duration.Companion.seconds

object GitHubPluginPullRequestViewer : PullRequestViewer<Project> {
    private val waitForGitHubPlugin = 3.seconds

    override suspend fun hasRepository(project: Project, repository: RepositoryName): Boolean =
        failingSafe("check ${project.name} for $repository") { mappingFor(project, repository) != null }

    override suspend fun show(project: Project, pullRequest: PullRequestRef): Boolean =
        failingSafe("show $pullRequest in ${project.name}") {
            val mapping = mappingFor(project, pullRequest.repository) ?: return@failingSafe false
            val account = accountFor(mapping) ?: return@failingSafe false
            val pullRequests = project.service<GHPRProjectViewModel>()
            // Not activateAndAwaitProject: its signature differs between 2026.2 and 2026.3.
            pullRequests.activate()
            pullRequests.selectorVm.selectRepoAndAccount(mapping, account)
            pullRequests.selectorVm.submitSelection()
            val connected = withTimeoutOrNull(waitForGitHubPlugin) {
                pullRequests.connectedProjectVm.filterNotNull().first { it.repository.isNamed(pullRequest.repository) }
            } ?: return@failingSafe false
            connected.openPullRequestInfoAndTimeline(pullRequest.number)
            true
        }

    /** The plugin detects remotes in the background, so a freshly opened project starts with none. */
    private suspend fun mappingFor(project: Project, repository: RepositoryName): GHGitRepositoryMapping? {
        if (project.isDisposed) return null
        val mappings = withTimeoutOrNull(waitForGitHubPlugin) {
            project.service<GHPRProjectViewModel>().selectorVm.repositoriesState.first { it.isNotEmpty() }
        }
        return mappings?.find { it.repository.isNamed(repository) }
    }

    private fun accountFor(mapping: GHGitRepositoryMapping): GithubAccount? =
        service<GHAccountManager>().accountsState.value.firstOrNull {
            URIUtil.equalWithoutSchema(it.server.toURI(), mapping.repository.serverPath.toURI())
        }

    private fun GHRepositoryCoordinates.isNamed(name: RepositoryName): Boolean =
        repositoryPath.owner.equals(name.owner, ignoreCase = true) &&
            repositoryPath.repository.equals(name.name, ignoreCase = true)

    /** Catches [LinkageError] too, so a future IDE that changes these internal APIs falls back instead of crashing. */
    private inline fun failingSafe(action: String, block: () -> Boolean): Boolean =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            thisLogger().warn("Could not $action in the GitHub pull request view", failure)
            false
        }
}
