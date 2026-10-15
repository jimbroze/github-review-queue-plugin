package io.github.jimdickinson.reviewqueue.ide

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef
import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import io.github.jimdickinson.reviewqueue.toolwindow.PullRequestOpener
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class IdeFirstPullRequestOpenerTest {

    private val repository = RepositoryName("acme", "rockets")
    private val pullRequest = PullRequestRef(repository, number = 42)
    private val url = "https://github.com/acme/rockets/pull/42"

    private val current = FakeProject("current")
    private val other = FakeProject("other")
    private val another = FakeProject("another")

    private val viewer = FakeViewer()
    private val projectsWithoutGitRemotes = mutableSetOf<FakeProject>()
    private val windowsBroughtToFront = mutableListOf<FakeProject>()
    private val urlsOpenedInBrowser = mutableListOf<String>()

    @Test
    fun `a pull request from the current project's repository opens in its pull request view`() {
        viewer.hasRepositoryIn(current)

        open(openProjects = listOf(current, other))

        assertEquals(listOf(current to pullRequest), viewer.shown)
        assertEquals(emptyList<String>(), urlsOpenedInBrowser)
    }

    @Test
    fun `opening a pull request in the current project leaves the window order alone`() {
        viewer.hasRepositoryIn(current)

        open(openProjects = listOf(current, other))

        assertEquals(emptyList<FakeProject>(), windowsBroughtToFront)
    }

    @Test
    fun `a pull request from another open project's repository opens there and brings its window to the front`() {
        viewer.hasRepositoryIn(other)

        open(openProjects = listOf(current, other))

        assertEquals(listOf(other to pullRequest), viewer.shown)
        assertEquals(listOf(other), windowsBroughtToFront)
        assertEquals(emptyList<String>(), urlsOpenedInBrowser)
    }

    @Test
    fun `the current project is preferred when several open projects have the repository`() {
        viewer.hasRepositoryIn(other, current, another)

        open(openProjects = listOf(other, current, another))

        assertEquals(listOf(current to pullRequest), viewer.shown)
        assertEquals(emptyList<FakeProject>(), windowsBroughtToFront)
    }

    @Test
    fun `the first open project with the repository is used when the current project does not have it`() {
        viewer.hasRepositoryIn(another, other)

        open(openProjects = listOf(current, other, another))

        assertEquals(listOf(other to pullRequest), viewer.shown)
        assertEquals(listOf(other), windowsBroughtToFront)
    }

    @Test
    fun `a pull request from a repository no open project has opens in the browser`() {
        open(openProjects = listOf(current, other))

        assertEquals(listOf(url), urlsOpenedInBrowser)
        assertEquals(emptyList<Pair<FakeProject, PullRequestRef>>(), viewer.shown)
        assertEquals(emptyList<FakeProject>(), windowsBroughtToFront)
    }

    @Test
    fun `when the IDE cannot show the pull request it opens in the browser`() {
        viewer.hasRepositoryIn(current)
        viewer.cannotShowIn(current)

        open(openProjects = listOf(current))

        assertEquals(listOf(url), urlsOpenedInBrowser)
    }

    @Test
    fun `when another project cannot show the pull request it opens in the browser`() {
        viewer.hasRepositoryIn(other)
        viewer.cannotShowIn(other)

        open(openProjects = listOf(current, other))

        assertEquals(listOf(url), urlsOpenedInBrowser)
    }

    @Test
    fun `a project without git remotes is never asked whether it has the repository`() {
        projectsWithoutGitRemotes += other
        viewer.hasRepositoryIn(another)

        open(openProjects = listOf(current, other, another))

        assertFalse(other in viewer.askedForRepository)
        assertEquals(listOf(another to pullRequest), viewer.shown)
    }

    @Test
    fun `a pull request opens in the browser when no open project has git remotes`() {
        projectsWithoutGitRemotes += listOf(current, other)

        open(openProjects = listOf(current, other))

        assertEquals(emptyList<FakeProject>(), viewer.askedForRepository)
        assertEquals(listOf(url), urlsOpenedInBrowser)
    }

    private fun open(openProjects: List<FakeProject>) = runBlocking {
        IdeFirstPullRequestOpener(
            currentProject = current,
            openProjects = { openProjects },
            hasGitRemotes = { it !in projectsWithoutGitRemotes },
            viewer = viewer,
            bringToFront = { windowsBroughtToFront += it },
            browser = PullRequestOpener { _, openedUrl -> urlsOpenedInBrowser += openedUrl },
        ).open(pullRequest, url)
    }

    private data class FakeProject(val name: String)

    private class FakeViewer : PullRequestViewer<FakeProject> {
        private val projectsWithRepository = mutableSetOf<FakeProject>()
        private val projectsThatCannotShow = mutableSetOf<FakeProject>()
        val shown = mutableListOf<Pair<FakeProject, PullRequestRef>>()
        val askedForRepository = mutableListOf<FakeProject>()

        fun hasRepositoryIn(vararg projects: FakeProject) {
            projectsWithRepository += projects
        }

        fun cannotShowIn(project: FakeProject) {
            projectsThatCannotShow += project
        }

        override suspend fun hasRepository(project: FakeProject, repository: RepositoryName): Boolean {
            askedForRepository += project
            return project in projectsWithRepository
        }

        override suspend fun show(project: FakeProject, pullRequest: PullRequestRef): Boolean {
            if (project in projectsThatCannotShow) return false
            shown += project to pullRequest
            return true
        }
    }
}
