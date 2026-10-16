package com.jimbroze.reviewqueue.worktime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.EnumSet

class WorkHoursTest {

    private val london = ZoneId.of("Europe/London")
    private val newYork = ZoneId.of("America/New_York")
    private val weekdays: Set<DayOfWeek> = EnumSet.range(MONDAY, FRIDAY)
    private val nineToFive = WorkHours(LocalTime.of(9, 0), LocalTime.of(17, 0), weekdays, london)

    @Test
    fun `time within one working day all counts`() {
        assertEquals(hours(3), nineToFive.workingTimeBetween(at("2026-10-12T10:00"), at("2026-10-12T13:00")))
    }

    @Test
    fun `time before the working day starts does not count`() {
        assertEquals(hours(1), nineToFive.workingTimeBetween(at("2026-10-12T06:30"), at("2026-10-12T10:00")))
    }

    @Test
    fun `time after the working day ends does not count`() {
        assertEquals(hours(1), nineToFive.workingTimeBetween(at("2026-10-12T16:00"), at("2026-10-12T22:00")))
    }

    @Test
    fun `overnight only the end of one day and the start of the next count`() {
        assertEquals(hours(3), nineToFive.workingTimeBetween(at("2026-10-12T15:00"), at("2026-10-13T10:00")))
    }

    @Test
    fun `days that are not work days do not count`() {
        assertEquals(hours(2), nineToFive.workingTimeBetween(at("2026-10-16T16:00"), at("2026-10-19T10:00")))
    }

    @Test
    fun `time entirely outside working hours is zero`() {
        assertEquals(Duration.ZERO, nineToFive.workingTimeBetween(at("2026-10-17T09:00"), at("2026-10-18T17:00")))
    }

    @Test
    fun `a whole working week is five working days`() {
        assertEquals(hours(40), nineToFive.workingTimeBetween(at("2026-10-12T00:00"), at("2026-10-19T00:00")))
    }

    @Test
    fun `several weeks add up`() {
        assertEquals(hours(3 * 40 + 1), nineToFive.workingTimeBetween(at("2026-10-12T09:00"), at("2026-11-02T10:00")))
    }

    @Test
    fun `an end before the start is zero`() {
        assertEquals(Duration.ZERO, nineToFive.workingTimeBetween(at("2026-10-13T12:00"), at("2026-10-12T12:00")))
    }

    @Test
    fun `chosen work days count even at the weekend`() {
        val withSaturday = WorkHours(LocalTime.of(9, 0), LocalTime.of(17, 0), weekdays + SATURDAY, london)

        assertEquals(hours(9), withSaturday.workingTimeBetween(at("2026-10-16T16:00"), at("2026-10-19T00:00")))
    }

    @Test
    fun `working hours are in the configured time zone`() {
        val nineToFiveInNewYork = WorkHours(LocalTime.of(9, 0), LocalTime.of(17, 0), weekdays, newYork)

        assertEquals(hours(1), nineToFiveInNewYork.workingTimeBetween(at("2026-10-12T09:00"), at("2026-10-12T15:00")))
    }

    @Test
    fun `working days stay the same length across a clock change`() {
        assertEquals(hours(8), nineToFive.workingTimeBetween(at("2026-10-23T13:00"), at("2026-10-26T13:00")))
    }

    @Test
    fun `the length of a working day is the time from start to end`() {
        assertEquals(Duration.ofMinutes(7 * 60 + 30), WorkHours(LocalTime.of(9, 0), LocalTime.of(16, 30), weekdays, london).dayLength)
    }

    @Test
    fun `office hours are nine to five on weekdays`() {
        assertEquals(nineToFive, WorkHours.officeHours(london))
    }

    @Test
    fun `a working day must end after it starts`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkHours(LocalTime.of(17, 0), LocalTime.of(9, 0), weekdays, london)
        }
        assertThrows(IllegalArgumentException::class.java) {
            WorkHours(LocalTime.of(9, 0), LocalTime.of(9, 0), weekdays, london)
        }
    }

    @Test
    fun `there must be at least one work day`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkHours(LocalTime.of(9, 0), LocalTime.of(17, 0), emptySet<DayOfWeek>(), london)
        }
    }

    private fun at(londonTime: String): Instant = LocalDateTime.parse(londonTime).atZone(london).toInstant()

    private fun hours(count: Int): Duration = Duration.ofHours(count.toLong())
}
