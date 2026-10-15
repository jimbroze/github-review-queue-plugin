package io.github.jimdickinson.reviewqueue.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class DurationTextTest {

    @Test
    fun `under a minute is less than a minute`() {
        assertEquals("less than a minute", DurationText.of(Duration.ofSeconds(59)))
    }

    @Test
    fun `a negative duration is less than a minute`() {
        assertEquals("less than a minute", DurationText.of(Duration.ofMinutes(-5)))
    }

    @Test
    fun `exactly one minute is 1 minute`() {
        assertEquals("1 minute", DurationText.of(Duration.ofMinutes(1)))
    }

    @Test
    fun `minutes are counted in whole minutes`() {
        assertEquals("59 minutes", DurationText.of(Duration.ofMinutes(59).plusSeconds(59)))
    }

    @Test
    fun `exactly one hour is 1 hour`() {
        assertEquals("1 hour", DurationText.of(Duration.ofHours(1)))
    }

    @Test
    fun `hours are counted in whole hours`() {
        assertEquals("23 hours", DurationText.of(Duration.ofHours(23).plusMinutes(59)))
    }

    @Test
    fun `exactly one day is 1 day`() {
        assertEquals("1 day", DurationText.of(Duration.ofDays(1)))
    }

    @Test
    fun `days are counted in whole days`() {
        assertEquals("3 days", DurationText.of(Duration.ofDays(3).plusHours(23)))
    }

    @Test
    fun `long waits are still counted in days`() {
        assertEquals("45 days", DurationText.of(Duration.ofDays(45)))
    }
}
