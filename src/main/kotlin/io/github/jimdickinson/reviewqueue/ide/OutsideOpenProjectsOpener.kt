package io.github.jimdickinson.reviewqueue.ide

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import io.github.jimdickinson.reviewqueue.toolwindow.PullRequestOpener
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
        TODO()
    }
}
