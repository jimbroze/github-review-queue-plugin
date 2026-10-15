package io.github.jimdickinson.reviewqueue.clones

import java.nio.file.Path

/**
 * Finds Git repositories under a code folder, at most [maxDepth] folders below it, without following links.
 * It does not look inside repositories, hidden folders, or dependency and build output folders.
 */
class CodeFolderScan(private val maxDepth: Int = 3) {
    fun repositoriesUnder(root: Path): List<Path> = TODO()
}
