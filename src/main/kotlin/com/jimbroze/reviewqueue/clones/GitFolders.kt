package com.jimbroze.reviewqueue.clones

import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

object GitFolders {
    private const val DOT_GIT = ".git"
    private const val GITDIR_PREFIX = "gitdir:"
    private val section = Regex("""^\s*\[.*]\s*$""")
    private val remoteSection = Regex("""^\s*\[\s*remote\s+"[^"]*"\s*]\s*$""", RegexOption.IGNORE_CASE)
    private val urlEntry = Regex("""^\s*url\s*=\s*(.*?)\s*$""", RegexOption.IGNORE_CASE)

    /** True when [folder] has a `.git` directory, or a `.git` file as worktrees and submodules do. */
    fun isRepository(folder: Path): Boolean = Files.exists(folder.resolve(DOT_GIT))

    /** The nearest repository containing [folder], or [folder] itself; [stopAt] and its ancestors are not considered. */
    fun enclosingRepository(folder: Path, stopAt: Path): Path? {
        val stop = stopAt.toAbsolutePath().normalize()
        return generateSequence(folder.toAbsolutePath().normalize()) { it.parent }
            .takeWhile { !stop.startsWith(it) }
            .firstOrNull(::isRepository)
    }

    /** The URLs of every remote; empty when [folder] is not a repository or its config cannot be read. */
    fun remoteUrls(folder: Path): List<String> =
        try {
            configFile(folder)?.let { remoteUrlsIn(Files.readAllLines(it)) } ?: emptyList()
        } catch (unreadable: IOException) {
            emptyList()
        } catch (malformed: InvalidPathException) {
            emptyList()
        }

    private fun configFile(folder: Path): Path? {
        val gitDir = gitDir(folder) ?: return null
        return sharedGitDir(gitDir).resolve("config").takeIf { Files.isRegularFile(it) }
    }

    private fun gitDir(folder: Path): Path? {
        val dotGit = folder.resolve(DOT_GIT)
        return when {
            Files.isDirectory(dotGit) -> dotGit
            Files.isRegularFile(dotGit) -> pointedToGitDir(folder, Files.readAllLines(dotGit))
            else -> null
        }
    }

    private fun pointedToGitDir(folder: Path, dotGitFileLines: List<String>): Path? =
        dotGitFileLines.firstOrNull { it.startsWith(GITDIR_PREFIX) }
            ?.removePrefix(GITDIR_PREFIX)
            ?.trim()
            ?.let { folder.resolve(it).normalize() }

    /** A worktree's git dir names the main repository's git dir, which holds the config, in its `commondir` file. */
    private fun sharedGitDir(gitDir: Path): Path {
        val commonDir = gitDir.resolve("commondir")
        if (!Files.isRegularFile(commonDir)) return gitDir
        return gitDir.resolve(Files.readString(commonDir).trim()).normalize()
    }

    private fun remoteUrlsIn(configLines: List<String>): List<String> {
        val urls = mutableListOf<String>()
        var inRemoteSection = false
        for (line in configLines) {
            if (section.matches(line)) {
                inRemoteSection = remoteSection.matches(line)
            } else if (inRemoteSection) {
                urlEntry.matchEntire(line)?.let { urls += it.groupValues[1] }
            }
        }
        return urls
    }
}
