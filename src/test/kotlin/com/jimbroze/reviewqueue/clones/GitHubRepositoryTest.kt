package com.jimbroze.reviewqueue.clones

import com.jimbroze.reviewqueue.queue.RepositoryName
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubRepositoryTest {

    private val defaultAliases = "github.com"
    private val kbus = RepositoryName("jimbroze", "kbus")

    private fun onGitHub(aliasesSetting: String = defaultAliases) =
        GitHubRepository(kbus, GitHubHosts.of("github.com", aliasesSetting))

    @Test
    fun `a remote on github com for the repository matches`() {
        assertTrue(onGitHub().isClonedFrom("https://github.com/jimbroze/kbus.git"))
        assertTrue(onGitHub().isClonedFrom("git@github.com:jimbroze/kbus.git"))
    }

    @Test
    fun `owner and name match regardless of case`() {
        assertTrue(onGitHub().isClonedFrom("git@github.com:JimBroze/KBus.git"))
    }

    @Test
    fun `a remote for another repository does not match`() {
        assertFalse(onGitHub().isClonedFrom("git@github.com:jimbroze/kbus-docs.git"))
        assertFalse(onGitHub().isClonedFrom("git@github.com:someone-else/kbus.git"))
    }

    @Test
    fun `a remote on another host does not match`() {
        assertFalse(onGitHub().isClonedFrom("git@gitlab.com:jimbroze/kbus.git"))
    }

    @Test
    fun `a remote that cannot be parsed does not match`() {
        assertFalse(onGitHub().isClonedFrom("not a url"))
    }

    @Test
    fun `an ssh remote using a host alias matches the GitHub server`() {
        val repository = onGitHub(aliasesSetting = "github.com, github.com-jimbroze")

        assertTrue(repository.isClonedFrom("git@github.com-jimbroze:jimbroze/kbus.git"))
    }

    @Test
    fun `an ssh host alias that is not configured does not match`() {
        assertFalse(onGitHub().isClonedFrom("git@github.com-jimbroze:jimbroze/kbus.git"))
    }

    @Test
    fun `an alias written as alias=server matches only that server`() {
        val aliasesSetting = "github.com,work=github.com,corp=ghe.example.com"

        assertTrue(onGitHub(aliasesSetting).isClonedFrom("git@work:jimbroze/kbus.git"))
        assertFalse(onGitHub(aliasesSetting).isClonedFrom("git@corp:jimbroze/kbus.git"))
    }

    @Test
    fun `alias hosts match regardless of case and surrounding spaces`() {
        val repository = onGitHub(aliasesSetting = " GitHub.com-JimBroze , github.com ")

        assertTrue(repository.isClonedFrom("git@github.com-jimbroze:jimbroze/kbus.git"))
    }

    @Test
    fun `a remote on a GitHub Enterprise server matches that server`() {
        val repository = GitHubRepository(kbus, GitHubHosts.of("ghe.example.com", defaultAliases))

        assertTrue(repository.isClonedFrom("https://ghe.example.com/jimbroze/kbus.git"))
        assertFalse(repository.isClonedFrom("https://github.com/jimbroze/kbus.git"))
    }
}
