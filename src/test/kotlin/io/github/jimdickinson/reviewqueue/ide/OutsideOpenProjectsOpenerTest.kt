package io.github.jimdickinson.reviewqueue.ide

import io.github.jimdickinson.reviewqueue.ide.OpenChoice.Clone
import io.github.jimdickinson.reviewqueue.ide.OpenChoice.OpenClone
import io.github.jimdickinson.reviewqueue.ide.OpenChoice.OpenInBrowser
import io.github.jimdickinson.reviewqueue.ide.OpenFailure.CouldNotClone
import io.github.jimdickinson.reviewqueue.ide.OpenFailure.CouldNotOpenFolder
import io.github.jimdickinson.reviewqueue.ide.OpenFailure.CouldNotShow
import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import io.github.jimdickinson.reviewqueue.toolwindow.PullRequestOpener
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class OutsideOpenProjectsOpenerTest {

    private val kbus = RepositoryName("jimbroze", "kbus")
    private val pullRequest = PullRequestRef(kbus, number = 7)
    private val url = "https://github.com/jimbroze/kbus/pull/7"
    private val code = Path.of("/Users/jim/Code")
    private val recentClone = Path.of("/Users/jim/Kotlin/kbus")
    private val scannedClone = Path.of("/Users/jim/Code/kbus-fork")

    private val dialogs = FakeDialogs()
    private val folders = FakeFolders()
    private val shown = mutableListOf<Pair<FakeProject, PullRequestRef>>()
    private val projectsThatCannotShow = mutableSetOf<FakeProject>()
    private val urlsOpenedInBrowser = mutableListOf<String>()
    private val failures = mutableListOf<OpenFailure>()

    @Test
    fun `each local clone is offered, then cloning into the first code folder, then the browser`() {
        open(clones = listOf(recentClone, scannedClone))

        assertEquals(listOf(OpenClone(recentClone), OpenClone(scannedClone), Clone(kbus, code), OpenInBrowser), dialogs.offered)
    }

    @Test
    fun `with no local clones, cloning and the browser are offered`() {
        open(clones = emptyList())

        assertEquals(listOf(Clone(kbus, code), OpenInBrowser), dialogs.offered)
    }

    @Test
    fun `cancelling the choice does nothing`() {
        dialogs.pick = null

        open(clones = listOf(recentClone))

        assertNothingHappened()
    }

    @Test
    fun `choosing a clone opens it as a project and shows the pull request there`() {
        dialogs.pick = OpenClone(recentClone)

        open(clones = listOf(recentClone))

        assertEquals(listOf(FakeProject(recentClone) to pullRequest), shown)
        assertEquals(emptyList<String>(), urlsOpenedInBrowser)
    }

    @Test
    fun `when the clone cannot be opened the pull request opens in the browser and the failure is reported`() {
        dialogs.pick = OpenClone(recentClone)
        folders.cannotOpen.add(recentClone)

        open(clones = listOf(recentClone))

        assertEquals(listOf(url), urlsOpenedInBrowser)
        assertEquals(listOf(CouldNotOpenFolder(recentClone)), failures)
    }

    @Test
    fun `when the opened project cannot show the pull request it opens in the browser and the failure is reported`() {
        dialogs.pick = OpenClone(recentClone)
        projectsThatCannotShow += FakeProject(recentClone)

        open(clones = listOf(recentClone))

        assertEquals(listOf(url), urlsOpenedInBrowser)
        assertEquals(listOf(CouldNotShow(pullRequest)), failures)
    }

    @Test
    fun `choosing to clone asks where, suggesting the offered folder`() {
        dialogs.pick = Clone(kbus, code)

        open(clones = emptyList())

        assertEquals(listOf(code), dialogs.suggestedParentFolders)
    }

    @Test
    fun `cloning goes into a folder named after the repository, which is then opened and shows the pull request`() {
        val work = Path.of("/Users/jim/Work")
        dialogs.pick = Clone(kbus, code)
        dialogs.parentFolder = work

        open(clones = emptyList())

        assertEquals(listOf(kbus to work.resolve("kbus")), folders.cloned)
        assertEquals(listOf(FakeProject(work.resolve("kbus")) to pullRequest), shown)
    }

    @Test
    fun `cloning goes into the first free folder when the repository's name is taken`() {
        dialogs.pick = Clone(kbus, code)
        folders.existing += listOf(code.resolve("kbus"), code.resolve("kbus-2"))

        open(clones = emptyList())

        assertEquals(listOf(kbus to code.resolve("kbus-3")), folders.cloned)
    }

    @Test
    fun `cancelling the choice of where to clone does nothing`() {
        dialogs.pick = Clone(kbus, code)
        dialogs.cancelsParentFolder = true

        open(clones = emptyList())

        assertEquals(emptyList<Pair<RepositoryName, Path>>(), folders.cloned)
        assertNothingHappened()
    }

    @Test
    fun `when cloning fails the pull request opens in the browser and the failure is reported`() {
        dialogs.pick = Clone(kbus, code)
        folders.cloningFails = true

        open(clones = emptyList())

        assertEquals(listOf(url), urlsOpenedInBrowser)
        assertEquals(listOf(CouldNotClone(kbus)), failures)
        assertEquals(emptyList<FakeProject>(), folders.opened)
    }

    @Test
    fun `choosing the browser opens the pull request in the browser`() {
        dialogs.pick = OpenInBrowser

        open(clones = listOf(recentClone))

        assertEquals(listOf(url), urlsOpenedInBrowser)
        assertEquals(emptyList<OpenFailure>(), failures)
    }

    private fun assertNothingHappened() {
        assertEquals(emptyList<FakeProject>(), folders.opened)
        assertEquals(emptyList<String>(), urlsOpenedInBrowser)
        assertEquals(emptyList<OpenFailure>(), failures)
    }

    private fun open(clones: List<Path>) = runBlocking {
        OutsideOpenProjectsOpener(
            findClones = { repository -> if (repository == kbus) clones else emptyList() },
            suggestedCloneParentFolder = { code },
            dialogs = dialogs,
            folders = folders,
            showWhenReady = { project, shownPullRequest ->
                (project !in projectsThatCannotShow).also { if (it) shown += project to shownPullRequest }
            },
            browser = PullRequestOpener { _, openedUrl -> urlsOpenedInBrowser += openedUrl },
            reportFailure = { failures += it },
        ).open(pullRequest, url)
    }

    private data class FakeProject(val folder: Path)

    private class FakeDialogs : OpenChoiceDialogs {
        var pick: OpenChoice? = OpenInBrowser
        var parentFolder: Path? = null
        val offered = mutableListOf<OpenChoice>()
        val suggestedParentFolders = mutableListOf<Path>()

        override suspend fun pick(choices: List<OpenChoice>): OpenChoice? {
            offered += choices
            return pick
        }

        override suspend fun pickCloneParentFolder(suggested: Path): Path? {
            suggestedParentFolders.add(suggested)
            return if (cancelsParentFolder) null else parentFolder ?: suggested
        }

        var cancelsParentFolder = false
    }

    private class FakeFolders : LocalFolders<FakeProject> {
        val existing = mutableSetOf<Path>()
        val cannotOpen = mutableSetOf<Path>()
        var cloningFails = false
        val cloned = mutableListOf<Pair<RepositoryName, Path>>()
        val opened = mutableListOf<FakeProject>()

        override fun exists(folder: Path): Boolean = folder in existing

        override suspend fun clone(repository: RepositoryName, into: Path): Boolean {
            if (cloningFails) return false
            cloned += repository to into
            return true
        }

        override suspend fun open(folder: Path): FakeProject? {
            if (folder in cannotOpen) return null
            return FakeProject(folder).also { opened += it }
        }
    }
}
