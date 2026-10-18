package com.jimbroze.reviewqueue.worktime

import java.time.Duration
import java.time.ZoneId

enum class Urgency { OnTime, NearlyOverdue, Overdue }

/**
 * How waiting time on a review request is measured, and how long a review may wait before it is
 * [Urgency.NearlyOverdue] or [Urgency.Overdue].
 *
 * @throws IllegalArgumentException if [nearlyOverdueAfter] is not positive, or [overdueAfter] is not after it.
 */
data class ReviewTiming(val workHours: WorkHours, val nearlyOverdueAfter: Duration, val overdueAfter: Duration) {
    init {
        require(nearlyOverdueAfter.isPositive) { "The nearly-overdue time must be positive" }
        require(overdueAfter > nearlyOverdueAfter) { "The overdue time must come after the nearly-overdue time" }
    }

    fun urgencyAfter(workingTimeWaited: Duration): Urgency = when {
        workingTimeWaited >= overdueAfter -> Urgency.Overdue
        workingTimeWaited >= nearlyOverdueAfter -> Urgency.NearlyOverdue
        else -> Urgency.OnTime
    }

    companion object {
        fun defaultsIn(zone: ZoneId): ReviewTiming =
            ReviewTiming(WorkHours.officeHours(zone), nearlyOverdueAfter = Duration.ofHours(3), overdueAfter = Duration.ofHours(4))
    }
}
