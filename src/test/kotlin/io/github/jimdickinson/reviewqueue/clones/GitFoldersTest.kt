package io.github.jimdickinson.reviewqueue.clones

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class GitFoldersTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `every remote of a clone is read`() {
        val fork = temp.clone(
            "kbus",
            "origin" to "git@github.com:jim/kbus.git",
            "upstream" to "https://github.com/jimbroze/kbus.git",
        )

        assertEquals(
            listOf("git@github.com:jim/kbus.git", "https://github.com/jimbroze/kbus.git"),
            GitFolders.remoteUrls(fork),
        )
    }

    @Test
    fun `a folder that is not a git repository has no remotes`() {
        val plain = temp.folder("notes")

        assertFalse(GitFolders.isRepository(plain))
        assertEquals(emptyList<String>(), GitFolders.remoteUrls(plain))
    }

    @Test
    fun `a missing folder has no remotes`() {
        assertEquals(emptyList<String>(), GitFolders.remoteUrls(temp.root.toPath().resolve("gone")))
    }

    @Test
    fun `a clone without remotes has none`() {
        assertEquals(emptyList<String>(), GitFolders.remoteUrls(temp.clone("local-only")))
    }

    @Test
    fun `a worktree reads the remotes of its main repository`() {
        val main = temp.clone("kbus", "origin" to "git@github.com:jimbroze/kbus.git")
        val worktreeGitDir = Files.createDirectories(main.resolve(".git/worktrees/feature"))
        Files.writeString(worktreeGitDir.resolve("commondir"), "../..\n")
        val worktree = temp.folder("kbus-feature")
        Files.writeString(worktree.resolve(".git"), "gitdir: $worktreeGitDir\n")

        assertTrue(GitFolders.isRepository(worktree))
        assertEquals(listOf("git@github.com:jimbroze/kbus.git"), GitFolders.remoteUrls(worktree))
    }

    @Test
    fun `a submodule reads the remotes from its relative git dir`() {
        val parent = temp.clone("app", "origin" to "git@github.com:acme/app.git")
        val moduleGitDir = Files.createDirectories(parent.resolve(".git/modules/kbus"))
        Files.writeString(moduleGitDir.resolve("config"), gitConfig("origin" to "git@github.com:jimbroze/kbus.git"))
        val submodule = temp.folder("app/libs/kbus")
        Files.writeString(submodule.resolve(".git"), "gitdir: ../../.git/modules/kbus\n")

        assertEquals(listOf("git@github.com:jimbroze/kbus.git"), GitFolders.remoteUrls(submodule))
    }

    @Test
    fun `remote section and key names are read regardless of case and spacing`() {
        val clone = temp.folder("kbus")
        Files.createDirectories(clone.resolve(".git"))
        Files.writeString(
            clone.resolve(".git/config"),
            "[Remote \"origin\"]\n    URL=git@github.com:jimbroze/kbus.git\n    pushurl = git@github.com:jim/kbus.git\n",
        )

        assertEquals(listOf("git@github.com:jimbroze/kbus.git"), GitFolders.remoteUrls(clone))
    }
}
