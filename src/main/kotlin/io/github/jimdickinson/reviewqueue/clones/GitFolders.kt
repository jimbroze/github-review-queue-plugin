package io.github.jimdickinson.reviewqueue.clones

import java.nio.file.Path

object GitFolders {
    /** True when [folder] has a `.git` directory, or a `.git` file as worktrees and submodules do. */
    fun isRepository(folder: Path): Boolean = TODO()

    /** The URLs of every remote; empty when [folder] is not a repository or its config cannot be read. */
    fun remoteUrls(folder: Path): List<String> = TODO()
}
