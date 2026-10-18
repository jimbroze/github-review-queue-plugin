package com.jimbroze.reviewqueue.clones

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path

class CodeFoldersTest {

    private val home = Path.of("/Users/jim")

    private fun toSearch(userFolders: List<String> = emptyList(), recentProjects: List<String> = emptyList()) =
        CodeFolders.toSearch(userFolders.map(Path::of), recentProjects.map(Path::of), home)

    @Test
    fun `the user's code folders come first, then the folders holding recent projects`() {
        val folders = toSearch(
            userFolders = listOf("/Users/jim/Work"),
            recentProjects = listOf("/Users/jim/Kotlin/review-queue", "/Users/jim/Code/kbus"),
        )

        assertEquals(listOf("/Users/jim/Work", "/Users/jim/Kotlin", "/Users/jim/Code").map(Path::of), folders)
    }

    @Test
    fun `each folder is searched once`() {
        val folders = toSearch(
            userFolders = listOf("/Users/jim/Code", "/Users/jim/Code/"),
            recentProjects = listOf("/Users/jim/Code/kbus", "/Users/jim/Code/rockets", "/Users/jim/Kotlin/../Code/app"),
        )

        assertEquals(listOf(Path.of("/Users/jim/Code")), folders)
    }

    @Test
    fun `the home folder and the file system root are never searched for holding a recent project`() {
        val folders = toSearch(recentProjects = listOf("/Users/jim/dotfiles", "/scratch"))

        assertEquals(emptyList<Path>(), folders)
    }

    @Test
    fun `a code folder the user lists is searched even when it is the home folder`() {
        assertEquals(listOf(home), toSearch(userFolders = listOf("/Users/jim")))
    }
}
