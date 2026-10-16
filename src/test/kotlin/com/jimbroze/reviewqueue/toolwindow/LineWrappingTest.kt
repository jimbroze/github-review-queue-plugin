package com.jimbroze.reviewqueue.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test

class LineWrappingTest {

    private fun wrap(pieces: List<String>, width: Int, gap: Int = 1, maxLines: Int = Int.MAX_VALUE) =
        wrapIntoLines(pieces, width, gap, maxLines) { it.length }

    @Test
    fun `pieces that fit the width share one line`() {
        assertEquals(listOf(listOf("aa", "bb", "cc")), wrap(listOf("aa", "bb", "cc"), width = 20))
    }

    @Test
    fun `a piece that would overflow the line starts the next one`() {
        assertEquals(listOf(listOf("aaa", "bbb"), listOf("ccc")), wrap(listOf("aaa", "bbb", "ccc"), width = 8))
    }

    @Test
    fun `the gap between pieces counts towards the width`() {
        assertEquals(listOf(listOf("aaa"), listOf("bbb")), wrap(listOf("aaa", "bbb"), width = 7, gap = 2))
    }

    @Test
    fun `pieces that exactly fill the width share a line`() {
        assertEquals(listOf(listOf("aaa", "bbb")), wrap(listOf("aaa", "bbb"), width = 7, gap = 1))
    }

    @Test
    fun `a piece wider than the width gets a line of its own`() {
        assertEquals(
            listOf(listOf("a"), listOf("too-wide"), listOf("b")),
            wrap(listOf("a", "too-wide", "b"), width = 4),
        )
    }

    @Test
    fun `once the most lines are used the rest go on the last line`() {
        assertEquals(
            listOf(listOf("aaa"), listOf("bbb", "ccc", "ddd")),
            wrap(listOf("aaa", "bbb", "ccc", "ddd"), width = 4, maxLines = 2),
        )
    }

    @Test
    fun `no pieces gives no lines`() {
        assertEquals(emptyList<List<String>>(), wrap(emptyList(), width = 10))
    }
}
