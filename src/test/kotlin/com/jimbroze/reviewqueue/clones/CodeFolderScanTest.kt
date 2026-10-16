package com.jimbroze.reviewqueue.clones

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class CodeFolderScanTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val code: Path get() = temp.root.toPath().resolve("Code")

    private fun scan(maxDepth: Int = 3) = CodeFolderScan(maxDepth).repositoriesUnder(code)

    @Test
    fun `repositories up to the scan depth are found`() {
        val shallow = temp.clone("Code/kbus")
        val deep = temp.clone("Code/work/acme/rockets")

        assertEquals(setOf(shallow, deep), scan().toSet())
    }

    @Test
    fun `folders deeper than the scan depth are not searched`() {
        temp.clone("Code/a/b/c/too-deep")

        assertEquals(emptyList<Path>(), scan())
    }

    @Test
    fun `the code folder itself is found when it is a repository`() {
        temp.clone("Code")

        assertEquals(listOf(code), scan())
    }

    @Test
    fun `a repository is not searched for nested repositories`() {
        val outer = temp.clone("Code/app")
        temp.clone("Code/app/vendor-copy/kbus")

        assertEquals(listOf(outer), scan())
    }

    @Test
    fun `node_modules is never searched`() {
        temp.clone("Code/node_modules/kbus")

        assertEquals(emptyList<Path>(), scan())
    }

    @Test
    fun `hidden folders are never searched`() {
        temp.clone("Code/.cache/kbus")

        assertEquals(emptyList<Path>(), scan())
    }

    @Test
    fun `build output folders are never searched`() {
        listOf("build", "out", "target", "dist").forEach { temp.clone("Code/$it/kbus") }

        assertEquals(emptyList<Path>(), scan())
    }

    @Test
    fun `symbolic links are not followed`() {
        val elsewhere = temp.clone("Elsewhere/kbus")
        Files.createDirectories(code)
        Files.createSymbolicLink(code.resolve("linked"), elsewhere.parent)

        assertEquals(emptyList<Path>(), scan())
    }

    @Test
    fun `a missing code folder has no repositories`() {
        assertEquals(emptyList<Path>(), scan())
    }
}
