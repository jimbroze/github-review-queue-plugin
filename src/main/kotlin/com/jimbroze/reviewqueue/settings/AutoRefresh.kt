package com.jimbroze.reviewqueue.settings

import java.time.Duration

/** Whether, and how often, the review queue fetches again by itself. */
sealed interface AutoRefresh {
    data object Off : AutoRefresh

    /** @throws IllegalArgumentException if [interval] is not positive. */
    data class Every(val interval: Duration) : AutoRefresh {
        init {
            require(interval.isPositive) { "The refresh interval must be positive" }
        }
    }

    companion object {
        const val DEFAULT_MINUTES = 5

        /** Returns the auto-refresh stored as these values; an interval under a minute becomes [DEFAULT_MINUTES]. */
        fun restore(isOn: Boolean, minutes: Int): AutoRefresh =
            if (isOn) Every(Duration.ofMinutes(minutes.takeIf { it >= 1 }?.toLong() ?: DEFAULT_MINUTES.toLong())) else Off
    }
}
