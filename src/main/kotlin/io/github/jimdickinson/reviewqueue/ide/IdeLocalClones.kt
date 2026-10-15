package io.github.jimdickinson.reviewqueue.ide

import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.advanced.AdvancedSettings
import io.github.jimdickinson.reviewqueue.clones.CodeFolders
import io.github.jimdickinson.reviewqueue.clones.GitHubHosts
import io.github.jimdickinson.reviewqueue.clones.GitHubRepository
import io.github.jimdickinson.reviewqueue.clones.LocalCloneFinder
import io.github.jimdickinson.reviewqueue.clones.pathOrNull
import io.github.jimdickinson.reviewqueue.github.preferredIdeAccount
import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import io.github.jimdickinson.reviewqueue.settings.ReviewQueueSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

/** Finds clones of a repository on this computer, using the IDE's recent projects, settings and GitHub account. */
object IdeLocalClones {
    private const val GITHUB_COM = "github.com"
    private const val ALIASES_SETTING_ID = "github.aliases"

    /** Returns no clones, rather than throwing, when the search fails. */
    suspend fun of(repository: RepositoryName): List<Path> = withContext(Dispatchers.IO) {
        try {
            val recentProjects = recentProjectPaths()
            LocalCloneFinder(
                recentProjects = { recentProjects },
                codeFolders = { codeFolders(recentProjects) },
                stopSearchingUpAt = home(),
            ).clonesOf(GitHubRepository(repository, gitHubHosts()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            thisLogger().warn("Could not search for clones of $repository", failure)
            emptyList()
        }
    }

    fun suggestedCloneParentFolder(): Path = codeFolders(recentProjectPaths()).firstOrNull() ?: home()

    private fun codeFolders(recentProjects: List<Path>): List<Path> =
        CodeFolders.toSearch(service<ReviewQueueSettings>().codeFolders, recentProjects, home())

    private fun home(): Path = Path.of(System.getProperty("user.home"))

    private fun recentProjectPaths(): List<Path> =
        RecentProjectsManagerBase.getInstanceEx().getRecentPaths().mapNotNull(::pathOrNull)

    private fun gitHubHosts(): GitHubHosts =
        GitHubHosts.of(preferredIdeAccount()?.server?.host ?: GITHUB_COM, hostAliasesSetting())

    private fun hostAliasesSetting(): String =
        try {
            AdvancedSettings.getString(ALIASES_SETTING_ID)
        } catch (unregistered: IllegalArgumentException) {
            GITHUB_COM
        }
}
