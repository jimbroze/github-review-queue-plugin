package com.jimbroze.reviewqueue.worktime

import java.time.ZoneId

/** How waiting time on a review request is measured. */
data class ReviewTiming(val workHours: WorkHours) {
    companion object {
        fun defaultsIn(zone: ZoneId): ReviewTiming = ReviewTiming(WorkHours.officeHours(zone))
    }
}
