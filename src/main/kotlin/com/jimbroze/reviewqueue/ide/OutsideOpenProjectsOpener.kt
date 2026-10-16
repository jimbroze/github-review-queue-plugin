package com.jimbroze.reviewqueue.ide

import com.jimbroze.reviewqueue.queue.PullRequestRef
import com.jimbroze.reviewqueue.queue.RepositoryName
import com.jimbroze.reviewqueue.toolwindow.PullRequestOpener
import java.nio.file.Path

sealed interface OpenChoice {
    data class OpenClone(val folder: Path) : OpenChoice
    data class Clone(val repository: RepositoryName, val parentFolder: Path) : OpenChoice
    data object OpenInBrowser : OpenChoice
}

sealed interface OpenFailure {
    data class CouldNotOpenFolder(val folder: Path) : OpenFailure
    data class CouldNotClone(val repository: RepositoryName) : OpenFailure
    data class CouldNotShow(val pullRequest: PullRequestRef) : OpenFailure
}

/** Asks the user; each function returns null when the user cancels. */
interface OpenChoiceDialogs {
    suspend fun pick(choices: List<OpenChoice>): OpenChoice?

    suspend fun pickCloneParentFolder(suggested: Path): Path?
}

interface LocalFolders<P> {
    fun exists(folder: Path): Boolean

    /** Returns false when cloning fails. */
    suspend fun clone(repository: RepositoryName, into: Path): Boolean

    /** Opens [folder] as a project, or returns null when it cannot. */
    suspend fun open(folder: Path): P?
}

/** Opens a pull request whose repository is in no open project: in a local clone, a new clone, or the browser. */
class OutsideOpenProjectsOpener<P>(
    private val findClones: suspend (RepositoryName) -> List<Path>,
    private val suggestedCloneParentFolder: () -> Path,
    private val dialogs: OpenChoiceDialogs,
    private val folders: LocalFolders<P>,
    private val showWhenReady: suspend (P, PullRequestRef) -> Boolean,
    private val browser: PullRequestOpener,
    private val reportFailure: (OpenFailure) -> Unit,
) {
    suspend fun open(pullRequest: PullRequestRef, url: String) {
        when (val choice = dialogs.pick(choicesFor(pullRequest.repository))) {
            is OpenChoice.OpenClone -> openAndShow(choice.folder, pullRequest, url)
            is OpenChoice.Clone -> cloneAndShow(choice, pullRequest, url)
            OpenChoice.OpenInBrowser -> browser.open(pullRequest, url)
            null -> Unit
        }
    }

    private suspend fun choicesFor(repository: RepositoryName): List<OpenChoice> =
        findClones(repository).map(OpenChoice::OpenClone) +
            OpenChoice.Clone(repository, suggestedCloneParentFolder()) +
            OpenChoice.OpenInBrowser

    private suspend fun cloneAndShow(choice: OpenChoice.Clone, pullRequest: PullRequestRef, url: String) {
        val parentFolder = dialogs.pickCloneParentFolder(choice.parentFolder) ?: return
        val target = freeFolderFor(choice.repository, parentFolder)
        if (folders.clone(choice.repository, target)) {
            openAndShow(target, pullRequest, url)
        } else {
            fallBackToBrowser(OpenFailure.CouldNotClone(choice.repository), pullRequest, url)
        }
    }

    private fun freeFolderFor(repository: RepositoryName, parentFolder: Path): Path =
        generateSequence(1) { it + 1 }
            .map { attempt -> parentFolder.resolve(if (attempt == 1) repository.name else "${repository.name}-$attempt") }
            .first { !folders.exists(it) }

    private suspend fun openAndShow(folder: Path, pullRequest: PullRequestRef, url: String) {
        val project = folders.open(folder)
        when {
            project == null -> fallBackToBrowser(OpenFailure.CouldNotOpenFolder(folder), pullRequest, url)
            !showWhenReady(project, pullRequest) -> fallBackToBrowser(OpenFailure.CouldNotShow(pullRequest), pullRequest, url)
        }
    }

    private fun fallBackToBrowser(failure: OpenFailure, pullRequest: PullRequestRef, url: String) {
        reportFailure(failure)
        browser.open(pullRequest, url)
    }
}
