package com.jimbroze.reviewqueue.toolwindow

import java.time.Duration

object WorkingTimeText {
    /** Returns [workingTime] in whole work days of [dayLength] once it reaches one, e.g. "2 work days"; otherwise as [DurationText]. */
    fun of(workingTime: Duration, dayLength: Duration): String {
        val workDays = workingTime.dividedBy(dayLength)
        return when {
            workDays == 1L -> "1 work day"
            workDays > 1L -> "$workDays work days"
            else -> DurationText.of(workingTime)
        }
    }
}
