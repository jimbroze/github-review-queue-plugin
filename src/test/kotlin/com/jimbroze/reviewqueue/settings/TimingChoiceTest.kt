package com.jimbroze.reviewqueue.settings

import com.jimbroze.reviewqueue.settings.TimingChoice.Problem
import com.jimbroze.reviewqueue.worktime.ReviewTiming
import com.jimbroze.reviewqueue.worktime.WorkHours
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId

class TimingChoiceTest {

    private val london = ZoneId.of("Europe/London")
    private val valid = TimingChoice(
        start = LocalTime.of(8, 0),
        end = LocalTime.of(16, 30),
        workDays = setOf(MONDAY, TUESDAY),
        nearlyOverdueHours = 2,
        overdueHours = 5,
    )

    @Test
    fun `the default choice is nine to five on weekdays, nearly overdue after 3 hours and overdue after 4`() {
        assertEquals(ReviewTiming.defaultsIn(london), TimingChoice.Default.toReviewTiming(london))
    }

    @Test
    fun `a valid choice has no problems`() {
        assertEquals(emptySet<Problem>(), valid.problems)
    }

    @Test
    fun `a valid choice becomes the review timing it describes`() {
        val expected = ReviewTiming(
            WorkHours(LocalTime.of(8, 0), LocalTime.of(16, 30), setOf(MONDAY, TUESDAY), london),
            nearlyOverdueAfter = Duration.ofHours(2),
            overdueAfter = Duration.ofHours(5),
        )

        assertEquals(expected, valid.toReviewTiming(london))
    }

    @Test
    fun `a working day that does not end after it starts is a problem`() {
        val choice = valid.copy(start = LocalTime.of(17, 0), end = LocalTime.of(9, 0))

        assertEquals(setOf(Problem.EndNotAfterStart), choice.problems)
    }

    @Test
    fun `choosing no work days is a problem`() {
        assertEquals(setOf(Problem.NoWorkDays), valid.copy(workDays = emptySet()).problems)
    }

    @Test
    fun `a nearly-overdue time under an hour is a problem`() {
        assertEquals(setOf(Problem.NearlyOverdueTooShort), valid.copy(nearlyOverdueHours = 0).problems)
    }

    @Test
    fun `an overdue time that is not after the nearly-overdue time is a problem`() {
        assertEquals(setOf(Problem.OverdueNotAfterNearlyOverdue), valid.copy(nearlyOverdueHours = 4, overdueHours = 4).problems)
    }

    @Test
    fun `a choice with problems uses the default review timing`() {
        assertEquals(ReviewTiming.defaultsIn(london), valid.copy(workDays = emptySet()).toReviewTiming(london))
    }

    @Test
    fun `a stored choice is restored`() {
        val restored = TimingChoice.restore("08:00", "16:30", listOf("MONDAY", "TUESDAY"), nearlyOverdueHours = 2, overdueHours = 5)

        assertEquals(valid, restored)
    }

    @Test
    fun `a restored choice survives being stored`() {
        val stored = TimingChoice.restore(valid.storedStart, valid.storedEnd, valid.storedWorkDays, valid.nearlyOverdueHours, valid.overdueHours)

        assertEquals(valid, stored)
    }

    @Test
    fun `stored times that cannot be read fall back to the default times`() {
        val restored = TimingChoice.restore("half past eight", null, listOf("MONDAY"), nearlyOverdueHours = 2, overdueHours = 5)

        assertEquals(TimingChoice.Default.start to TimingChoice.Default.end, restored.start to restored.end)
    }

    @Test
    fun `stored day names that are not days are left out`() {
        val restored = TimingChoice.restore("09:00", "17:00", listOf("SATURDAY", "Caturday", "SUNDAY"), 3, 4)

        assertEquals(setOf(SATURDAY, SUNDAY), restored.workDays)
    }

    @Test
    fun `no stored work days fall back to weekdays`() {
        val restored = TimingChoice.restore("09:00", "17:00", emptyList(), 3, 4)

        assertEquals(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), restored.workDays)
    }
}
