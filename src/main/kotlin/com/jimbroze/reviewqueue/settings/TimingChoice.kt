package com.jimbroze.reviewqueue.settings

import com.jimbroze.reviewqueue.worktime.ReviewTiming
import com.jimbroze.reviewqueue.worktime.WorkHours
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.EnumSet

/** Work hours and overdue times as chosen in settings, which may not yet make a valid [ReviewTiming]. */
data class TimingChoice(
    val start: LocalTime,
    val end: LocalTime,
    val workDays: Set<DayOfWeek>,
    val nearlyOverdueHours: Int,
    val overdueHours: Int,
) {
    enum class Problem { EndNotAfterStart, NoWorkDays, NearlyOverdueTooShort, OverdueNotAfterNearlyOverdue }

    val problems: Set<Problem>
        get() = buildSet {
            if (end <= start) add(Problem.EndNotAfterStart)
            if (workDays.isEmpty()) add(Problem.NoWorkDays)
            if (nearlyOverdueHours < 1) add(Problem.NearlyOverdueTooShort)
            if (overdueHours <= nearlyOverdueHours) add(Problem.OverdueNotAfterNearlyOverdue)
        }

    val storedStart: String get() = start.toString()
    val storedEnd: String get() = end.toString()
    val storedWorkDays: List<String> get() = workDays.sorted().map(DayOfWeek::name)

    /** Returns the timing this choice describes in [zone], or the default timing if the choice has [problems]. */
    fun toReviewTiming(zone: ZoneId): ReviewTiming =
        if (problems.isEmpty()) {
            ReviewTiming(
                WorkHours(start, end, workDays, zone),
                nearlyOverdueAfter = Duration.ofHours(nearlyOverdueHours.toLong()),
                overdueAfter = Duration.ofHours(overdueHours.toLong()),
            )
        } else {
            ReviewTiming.defaultsIn(zone)
        }

    companion object {
        val Default = TimingChoice(
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            workDays = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            nearlyOverdueHours = 3,
            overdueHours = 4,
        )

        /** Returns the choice stored as these values; values that cannot be read become their defaults. */
        fun restore(start: String?, end: String?, workDays: List<String>, nearlyOverdueHours: Int, overdueHours: Int): TimingChoice =
            TimingChoice(
                start = timeOrNull(start) ?: Default.start,
                end = timeOrNull(end) ?: Default.end,
                workDays = workDays.mapNotNull(::dayOrNull).toSet().ifEmpty { Default.workDays },
                nearlyOverdueHours = nearlyOverdueHours,
                overdueHours = overdueHours,
            )

        private fun timeOrNull(text: String?): LocalTime? =
            try {
                text?.let(LocalTime::parse)
            } catch (_: DateTimeParseException) {
                null
            }

        private fun dayOrNull(name: String): DayOfWeek? = DayOfWeek.entries.firstOrNull { it.name == name }
    }
}
