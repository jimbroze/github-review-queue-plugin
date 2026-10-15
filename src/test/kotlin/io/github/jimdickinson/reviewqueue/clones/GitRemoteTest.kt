package io.github.jimdickinson.reviewqueue.clones

import io.github.jimdickinson.reviewqueue.queue.RepositoryName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitRemoteTest {

    private val rockets = GitRemote("github.com", RepositoryName("acme", "rockets"))

    @Test
    fun `an https remote is parsed into host, owner and name`() {
        assertEquals(rockets, GitRemote.parse("https://github.com/acme/rockets.git"))
    }

    @Test
    fun `an https remote without the git suffix or with a trailing slash is parsed`() {
        assertEquals(rockets, GitRemote.parse("https://github.com/acme/rockets"))
        assertEquals(rockets, GitRemote.parse("https://github.com/acme/rockets/"))
        assertEquals(rockets, GitRemote.parse("https://github.com/acme/rockets.git/"))
    }

    @Test
    fun `an https remote with a user name is parsed`() {
        assertEquals(rockets, GitRemote.parse("https://jim@github.com/acme/rockets.git"))
    }

    @Test
    fun `an scp-style ssh remote is parsed`() {
        assertEquals(rockets, GitRemote.parse("git@github.com:acme/rockets.git"))
    }

    @Test
    fun `an ssh url remote is parsed, with or without a port`() {
        assertEquals(rockets, GitRemote.parse("ssh://git@github.com/acme/rockets.git"))
        assertEquals(rockets, GitRemote.parse("ssh://git@github.com:22/acme/rockets"))
    }

    @Test
    fun `an ssh host alias is kept as the host`() {
        assertEquals(
            GitRemote("github.com-jimbroze", RepositoryName("jimbroze", "kbus")),
            GitRemote.parse("git@github.com-jimbroze:jimbroze/kbus.git"),
        )
    }

    @Test
    fun `the host is lower case and owner and name keep their case`() {
        assertEquals(
            GitRemote("github.com", RepositoryName("Acme", "Rockets")),
            GitRemote.parse("https://GitHub.com/Acme/Rockets.git"),
        )
    }

    @Test
    fun `a remote that does not name exactly an owner and a repository is not parsed`() {
        assertNull(GitRemote.parse("https://gitlab.com/group/subgroup/project.git"))
        assertNull(GitRemote.parse("https://github.com/acme"))
        assertNull(GitRemote.parse("/Users/jim/Code/rockets.git"))
        assertNull(GitRemote.parse("../rockets"))
        assertNull(GitRemote.parse(""))
    }
}
