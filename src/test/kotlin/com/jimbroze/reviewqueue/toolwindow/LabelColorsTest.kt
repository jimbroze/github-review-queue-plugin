package com.jimbroze.reviewqueue.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test

class LabelColorsTest {

    private val white = 0xFFFFFF
    private val black = 0x000000

    @Test
    fun `a label's background is its own colour`() {
        assertEquals(0x0E8A16, labelColorsFor("0e8a16").background)
    }

    @Test
    fun `a dark label has white text`() {
        assertEquals(white, labelColorsFor("0e8a16").text)
    }

    @Test
    fun `a light label has black text`() {
        assertEquals(black, labelColorsFor("fef2c0").text)
    }

    @Test
    fun `a malformed colour falls back to the neutral colours`() {
        assertEquals(LabelColors.Neutral, labelColorsFor("zzz"))
    }

    @Test
    fun `an empty colour falls back to the neutral colours`() {
        assertEquals(LabelColors.Neutral, labelColorsFor(""))
    }
}
