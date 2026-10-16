package com.jimbroze.reviewqueue.worktime

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.EnumSet

/**
 * The hours someone works: from [start] to [end] local time in [zone], on each of [workDays].
 *
 * @throws IllegalArgumentException if [end] is not after [start], or there are no [workDays].
 */
data class WorkHours(val start: LocalTime, val end: LocalTime, val workDays: Set<DayOfWeek>, val zone: ZoneId) {
    init {
        require(end > start) { "A working day must end after it starts" }
        require(workDays.isNotEmpty()) { "There must be at least one work day" }
    }

    val dayLength: Duration get() = Duration.between(start, end)

    /** Returns how much of the time from [from] to [to] falls within these hours; zero if [to] is not after [from]. */
    fun workingTimeBetween(from: Instant, to: Instant): Duration =
        if (to <= from) {
            Duration.ZERO
        } else {
            datesSpanning(from, to)
                .filter { it.dayOfWeek in workDays }
                .fold(Duration.ZERO) { total, date -> total + overlap(workingWindowOn(date), from..to) }
        }

    private fun datesSpanning(from: Instant, to: Instant): Sequence<LocalDate> {
        val lastDate = LocalDate.ofInstant(to, zone)
        return generateSequence(LocalDate.ofInstant(from, zone)) { it.plusDays(1) }.takeWhile { it <= lastDate }
    }

    private fun workingWindowOn(date: LocalDate): ClosedRange<Instant> =
        date.atTime(start).atZone(zone).toInstant()..date.atTime(end).atZone(zone).toInstant()

    private fun overlap(first: ClosedRange<Instant>, second: ClosedRange<Instant>): Duration {
        val overlapStart = maxOf(first.start, second.start)
        val overlapEnd = minOf(first.endInclusive, second.endInclusive)
        return if (overlapEnd > overlapStart) Duration.between(overlapStart, overlapEnd) else Duration.ZERO
    }

    companion object {
        /** Nine to five, Monday to Friday, in [zone]. */
        fun officeHours(zone: ZoneId): WorkHours =
            WorkHours(LocalTime.of(9, 0), LocalTime.of(17, 0), EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), zone)
    }
}
