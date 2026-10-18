package com.jimbroze.reviewqueue.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.jimbroze.reviewqueue.settings.TimingChoice.Problem
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueBundle
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import javax.swing.JComponent

/** The settings rows for work hours and overdue times; [save] stores what was applied in [settings]. */
class TimingSection(private val settings: ReviewQueueSettings) {
    private var applied: TimingChoice? = null
    private lateinit var startBox: ComboBox<LocalTime>
    private lateinit var endBox: ComboBox<LocalTime>
    private val dayBoxes = mutableMapOf<DayOfWeek, JBCheckBox>()
    private lateinit var nearlyOverdueField: JBTextField
    private lateinit var overdueField: JBTextField

    fun addTo(panel: Panel) = with(panel) {
        group(ReviewQueueBundle.message("settings.timing")) {
            row(ReviewQueueBundle.message("settings.workHours")) {
                startBox = comboBox(selectableTimes()).bindItem({ settings.timing.start }, { time -> time?.let { edit { copy(start = it) } } }).component
                label(ReviewQueueBundle.message("settings.workHours.to"))
                endBox = comboBox(selectableTimes()).bindItem({ settings.timing.end }, { time -> time?.let { edit { copy(end = it) } } })
                    .validationOnInput { problemOn(it, Problem.EndNotAfterStart, "settings.problem.endNotAfterStart") }
                    .validationOnApply { problemOn(it, Problem.EndNotAfterStart, "settings.problem.endNotAfterStart") }
                    .component
            }
            row(ReviewQueueBundle.message("settings.workDays")) {
                DayOfWeek.entries.forEach { day ->
                    dayBoxes[day] = checkBox(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                        .bindSelected({ day in settings.timing.workDays }, { chosen -> edit { copy(workDays = workDays.with(day, chosen)) } })
                        .validationOnApply { problemOn(it, Problem.NoWorkDays, "settings.problem.noWorkDays") }
                        .component
                }
            }
            row(ReviewQueueBundle.message("settings.nearlyOverdueAfter")) {
                nearlyOverdueField = intTextField(1..HOURS_IN_A_WEEK).columns(4)
                    .bindIntText({ settings.timing.nearlyOverdueHours }, { edit { copy(nearlyOverdueHours = it) } })
                    .gap(RightGap.SMALL)
                    .component
                label(ReviewQueueBundle.message("settings.hours"))
            }
            row(ReviewQueueBundle.message("settings.overdueAfter")) {
                overdueField = intTextField(1..HOURS_IN_A_WEEK).columns(4)
                    .bindIntText({ settings.timing.overdueHours }, { edit { copy(overdueHours = it) } })
                    .validationOnInput { problemOn(it, Problem.OverdueNotAfterNearlyOverdue, "settings.problem.overdueNotAfterNearlyOverdue") }
                    .validationOnApply { problemOn(it, Problem.OverdueNotAfterNearlyOverdue, "settings.problem.overdueNotAfterNearlyOverdue") }
                    .gap(RightGap.SMALL)
                    .component
                label(ReviewQueueBundle.message("settings.hours"))
            }.comment(ReviewQueueBundle.message("settings.overdue.comment"))
        }
    }

    fun save() {
        applied?.let { settings.timing = it }
        applied = null
    }

    private fun edit(change: TimingChoice.() -> TimingChoice) {
        applied = (applied ?: settings.timing).change()
    }

    private fun problemOn(component: JComponent, problem: Problem, messageKey: String): ValidationInfo? =
        if (problem in typedChoice().problems) ValidationInfo(ReviewQueueBundle.message(messageKey), component) else null

    private fun typedChoice() = TimingChoice(
        start = startBox.item,
        end = endBox.item,
        workDays = dayBoxes.filterValues(JBCheckBox::isSelected).keys,
        nearlyOverdueHours = nearlyOverdueField.text.toIntOrNull() ?: 0,
        overdueHours = overdueField.text.toIntOrNull() ?: 0,
    )

    private fun selectableTimes(): List<LocalTime> =
        (settings.timing.let { listOf(it.start, it.end) } + halfHoursOfTheDay).distinct().sorted()

    private fun Set<DayOfWeek>.with(day: DayOfWeek, included: Boolean): Set<DayOfWeek> = if (included) this + day else this - day

    private companion object {
        const val HOURS_IN_A_WEEK = 168
        val halfHoursOfTheDay = (0 until 48).map { LocalTime.MIDNIGHT.plusMinutes(30L * it) }
    }
}
