package com.jimbroze.reviewqueue.clones

import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Finds Git repositories under a code folder, at most [maxDepth] folders below it, without following links.
 * It does not look inside repositories, hidden folders, or dependency and build output folders.
 */
class CodeFolderScan(private val maxDepth: Int = 3) {
    init {
        require(maxDepth >= 0) { "maxDepth must not be negative: $maxDepth" }
    }

    fun repositoriesUnder(root: Path): List<Path> = if (Files.isDirectory(root)) search(root, depth = 0) else emptyList()

    private fun search(folder: Path, depth: Int): List<Path> = when {
        GitFolders.isRepository(folder) -> listOf(folder)
        depth == maxDepth -> emptyList()
        else -> searchableSubfolders(folder).flatMap { search(it, depth + 1) }
    }

    private fun searchableSubfolders(folder: Path): List<Path> =
        try {
            Files.newDirectoryStream(folder).use { entries ->
                entries.filter { Files.isDirectory(it, NOFOLLOW_LINKS) && !isSkipped(it) }.sorted()
            }
        } catch (unreadable: IOException) {
            emptyList()
        }

    private fun isSkipped(folder: Path): Boolean = folder.name.startsWith(".") || folder.name in skippedNames

    private companion object {
        val skippedNames = setOf("node_modules", "build", "out", "target", "dist")
    }
}
