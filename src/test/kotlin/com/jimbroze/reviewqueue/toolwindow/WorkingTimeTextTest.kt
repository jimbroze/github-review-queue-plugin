package com.jimbroze.reviewqueue.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class WorkingTimeTextTest {

    private val eightHourDay = Duration.ofHours(8)

    @Test
    fun `less than a working day is shown in hours or minutes`() {
        assertEquals("7 hours", WorkingTimeText.of(Duration.ofMinutes(7 * 60 + 59), eightHourDay))
        assertEquals("45 minutes", WorkingTimeText.of(Duration.ofMinutes(45), eightHourDay))
    }

    @Test
    fun `no working time is less than a minute`() {
        assertEquals("less than a minute", WorkingTimeText.of(Duration.ZERO, eightHourDay))
    }

    @Test
    fun `exactly one working day is 1 work day`() {
        assertEquals("1 work day", WorkingTimeText.of(eightHourDay, eightHourDay))
    }

    @Test
    fun `work days are counted in whole days`() {
        assertEquals("2 work days", WorkingTimeText.of(Duration.ofHours(2 * 8 + 7), eightHourDay))
    }

    @Test
    fun `a work day is as long as the configured working day`() {
        val shortDay = Duration.ofMinutes(7 * 60 + 30)

        assertEquals("1 work day", WorkingTimeText.of(shortDay, shortDay))
        assertEquals("7 hours", WorkingTimeText.of(shortDay.minusMinutes(1), shortDay))
    }
}
