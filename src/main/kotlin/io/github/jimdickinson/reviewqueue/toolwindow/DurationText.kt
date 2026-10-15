package io.github.jimdickinson.reviewqueue.toolwindow

import java.time.Duration

object DurationText {
    /** Returns [duration] in its largest whole unit, e.g. "3 days"; anything under a minute is "less than a minute". */
    fun of(duration: Duration): String = when {
        duration.toDays() >= 1 -> count(duration.toDays(), "day")
        duration.toHours() >= 1 -> count(duration.toHours(), "hour")
        duration.toMinutes() >= 1 -> count(duration.toMinutes(), "minute")
        else -> "less than a minute"
    }

    private fun count(amount: Long, unit: String): String =
        if (amount == 1L) "1 $unit" else "$amount ${unit}s"
}
