package io.github.jimdickinson.reviewqueue.clones

import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class LocalCloneFinderTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val kbus = GitHubRepository(RepositoryName("jimbroze", "kbus"), GitHubHosts.of("github.com", "github.com"))
    private val kbusRemote = "origin" to "git@github.com:jimbroze/kbus.git"

    private fun clonesOf(recentProjects: List<Path> = emptyList(), codeFolders: List<Path> = emptyList()) =
        LocalCloneFinder({ recentProjects }, { codeFolders }, stopSearchingUpAt = temp.root.toPath()).clonesOf(kbus)

    @Test
    fun `a recent project inside a clone finds the enclosing clone`() {
        val monorepo = temp.clone("monorepo", kbusRemote)
        val app = temp.folder("monorepo/apps/web")

        assertEquals(listOf(monorepo), clonesOf(recentProjects = listOf(app)))
    }

    @Test
    fun `a recent project that is a clone of the repository is found`() {
        val recent = temp.clone("kbus", kbusRemote)

        assertEquals(listOf(recent), clonesOf(recentProjects = listOf(recent)))
    }

    @Test
    fun `a clone under a code folder is found`() {
        val scanned = temp.clone("Code/kbus", kbusRemote)

        assertEquals(listOf(scanned), clonesOf(codeFolders = listOf(temp.folder("Code"))))
    }

    @Test
    fun `a clone whose upstream remote is the repository is found`() {
        val fork = temp.clone(
            "Code/kbus-fork",
            "origin" to "git@github.com:jim/kbus.git",
            "upstream" to "https://github.com/jimbroze/kbus.git",
        )

        assertEquals(listOf(fork), clonesOf(codeFolders = listOf(temp.folder("Code"))))
    }

    @Test
    fun `clones of other repositories are left out`() {
        val other = temp.clone("Code/rockets", "origin" to "git@github.com:acme/rockets.git")

        assertEquals(emptyList<Path>(), clonesOf(recentProjects = listOf(other), codeFolders = listOf(temp.folder("Code"))))
    }

    @Test
    fun `a recent project that is not a git repository is ignored`() {
        val notes = temp.folder("notes")

        assertEquals(emptyList<Path>(), clonesOf(recentProjects = listOf(notes)))
    }

    @Test
    fun `a recent project that no longer exists is ignored`() {
        assertEquals(emptyList<Path>(), clonesOf(recentProjects = listOf(temp.root.toPath().resolve("deleted"))))
    }

    @Test
    fun `recent projects are listed before scanned folders, keeping the most recent first`() {
        val scanned = temp.clone("Code/a-kbus", kbusRemote)
        val older = temp.clone("old/kbus", kbusRemote)
        val newest = temp.clone("new/kbus", kbusRemote)

        val clones = clonesOf(recentProjects = listOf(newest, older), codeFolders = listOf(temp.folder("Code")))

        assertEquals(listOf(newest, older, scanned), clones)
    }

    @Test
    fun `a clone that is both a recent project and under a code folder is listed once`() {
        val clone = temp.clone("Code/kbus", kbusRemote)
        val sameFolderWrittenDifferently = temp.root.toPath().resolve("Code/../Code/kbus/")

        val clones = clonesOf(recentProjects = listOf(sameFolderWrittenDifferently), codeFolders = listOf(temp.folder("Code")))

        assertEquals(1, clones.size)
    }

    @Test
    fun `code folders are searched in the order given`() {
        val first = temp.clone("Work/kbus", kbusRemote)
        val second = temp.clone("Code/kbus", kbusRemote)

        assertEquals(listOf(first, second), clonesOf(codeFolders = listOf(temp.folder("Work"), temp.folder("Code"))))
    }
}
