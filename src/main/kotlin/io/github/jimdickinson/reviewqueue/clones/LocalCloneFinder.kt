package io.github.jimdickinson.reviewqueue.clones

import java.nio.file.Path

/**
 * Blocks on the file system; call it off the UI thread.
 * A recent project inside a repository counts as that repository, looking no higher than below [stopSearchingUpAt].
 */
class LocalCloneFinder(
    private val recentProjects: () -> List<Path>,
    private val codeFolders: () -> List<Path>,
    private val stopSearchingUpAt: Path,
    private val scan: CodeFolderScan = CodeFolderScan(),
) {
    /** Recent projects first, most recent first, then clones under the code folders; each folder once. */
    fun clonesOf(repository: GitHubRepository): List<Path> = TODO()
}
