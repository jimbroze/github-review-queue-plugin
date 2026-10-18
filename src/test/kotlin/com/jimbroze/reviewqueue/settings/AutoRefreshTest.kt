package com.jimbroze.reviewqueue.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration

class AutoRefreshTest {

    @Test
    fun `auto-refresh stored as off is off`() {
        assertEquals(AutoRefresh.Off, AutoRefresh.restore(isOn = false, minutes = 10))
    }

    @Test
    fun `auto-refresh stored as on refreshes every stored number of minutes`() {
        assertEquals(AutoRefresh.Every(Duration.ofMinutes(10)), AutoRefresh.restore(isOn = true, minutes = 10))
    }

    @Test
    fun `a stored interval under a minute falls back to every 5 minutes`() {
        assertEquals(AutoRefresh.Every(Duration.ofMinutes(5)), AutoRefresh.restore(isOn = true, minutes = 0))
    }

    @Test
    fun `an interval must be positive`() {
        assertThrows(IllegalArgumentException::class.java) { AutoRefresh.Every(Duration.ZERO) }
    }
}
