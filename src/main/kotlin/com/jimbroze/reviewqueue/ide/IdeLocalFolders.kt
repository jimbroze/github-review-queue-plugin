package com.jimbroze.reviewqueue.ide

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import git4idea.checkout.GitCheckoutProvider
import git4idea.commands.Git
import com.jimbroze.reviewqueue.github.preferredIdeAccount
import com.jimbroze.reviewqueue.queue.RepositoryName
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/** Clones and opens folders as projects; [project] hosts the progress and errors while cloning. */
class IdeLocalFolders(private val project: Project) : LocalFolders<Project> {
    override fun exists(folder: Path): Boolean = Files.exists(folder)

    override suspend fun clone(repository: RepositoryName, into: Path): Boolean {
        val parentFolder = into.parent ?: return false
        return failingSafe("clone $repository into $into") {
            withContext(Dispatchers.IO) { Files.createDirectories(parentFolder) }
            withBackgroundProgress(project, ReviewQueueBundle.message("clone.progress", nameOf(repository))) {
                coroutineToIndicator {
                    GitCheckoutProvider.doClone(project, Git.getInstance(), into.name, parentFolder.toString(), httpsUrlOf(repository))
                }
            }
        }
    }

    override suspend fun open(folder: Path): Project? =
        try {
            ProjectUtil.openOrImportAsync(folder)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            thisLogger().warn("Could not open $folder", failure)
            null
        }

    private fun httpsUrlOf(repository: RepositoryName): String {
        val serverUrl = preferredIdeAccount()?.server?.toUrl() ?: "https://github.com"
        return "$serverUrl/${nameOf(repository)}.git"
    }

    private fun nameOf(repository: RepositoryName) = "${repository.owner}/${repository.name}"

    private inline fun failingSafe(action: String, block: () -> Boolean): Boolean =
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            thisLogger().warn("Could not $action", failure)
            false
        }
}
