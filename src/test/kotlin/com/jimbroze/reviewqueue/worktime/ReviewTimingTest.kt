package com.jimbroze.reviewqueue.worktime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.ZoneOffset

class ReviewTimingTest {

    private val officeHours = WorkHours.officeHours(ZoneOffset.UTC)
    private val timing = ReviewTiming(officeHours, nearlyOverdueAfter = Duration.ofHours(3), overdueAfter = Duration.ofHours(4))

    @Test
    fun `a wait shorter than the nearly-overdue time is on time`() {
        assertEquals(Urgency.OnTime, timing.urgencyAfter(Duration.ofMinutes(2 * 60 + 59)))
    }

    @Test
    fun `a wait reaching the nearly-overdue time is nearly overdue`() {
        assertEquals(Urgency.NearlyOverdue, timing.urgencyAfter(Duration.ofHours(3)))
        assertEquals(Urgency.NearlyOverdue, timing.urgencyAfter(Duration.ofMinutes(3 * 60 + 59)))
    }

    @Test
    fun `a wait reaching the overdue time is overdue`() {
        assertEquals(Urgency.Overdue, timing.urgencyAfter(Duration.ofHours(4)))
        assertEquals(Urgency.Overdue, timing.urgencyAfter(Duration.ofHours(30)))
    }

    @Test
    fun `by default reviews are nearly overdue after 3 hours and overdue after 4 in office hours`() {
        assertEquals(ReviewTiming(officeHours, Duration.ofHours(3), Duration.ofHours(4)), ReviewTiming.defaultsIn(ZoneOffset.UTC))
    }

    @Test
    fun `the nearly-overdue time must be positive`() {
        assertThrows(IllegalArgumentException::class.java) { ReviewTiming(officeHours, Duration.ZERO, Duration.ofHours(4)) }
        assertThrows(IllegalArgumentException::class.java) { ReviewTiming(officeHours, Duration.ofHours(-1), Duration.ofHours(4)) }
    }

    @Test
    fun `the overdue time must come after the nearly-overdue time`() {
        assertThrows(IllegalArgumentException::class.java) { ReviewTiming(officeHours, Duration.ofHours(4), Duration.ofHours(4)) }
        assertThrows(IllegalArgumentException::class.java) { ReviewTiming(officeHours, Duration.ofHours(4), Duration.ofHours(3)) }
    }
}
