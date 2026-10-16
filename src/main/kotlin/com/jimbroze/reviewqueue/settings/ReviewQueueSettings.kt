package com.jimbroze.reviewqueue.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.jimbroze.reviewqueue.clones.pathOrNull
import com.jimbroze.reviewqueue.queue.SortOrder
import com.jimbroze.reviewqueue.toolwindow.QueuePreferences
import java.nio.file.Path
import java.time.ZoneId

@Service(Service.Level.APP)
@State(name = "ReviewQueueSettings", storages = [Storage("reviewQueue.xml")])
class ReviewQueueSettings : SimplePersistentStateComponent<ReviewQueueSettings.StoredSettings>(StoredSettings()) {
    class StoredSettings : BaseState() {
        var codeFolders by list<String>()
        var workStart by string(TimingChoice.Default.storedStart)
        var workEnd by string(TimingChoice.Default.storedEnd)
        var workDays by list<String>()
        var nearlyOverdueHours by property(TimingChoice.Default.nearlyOverdueHours)
        var overdueHours by property(TimingChoice.Default.overdueHours)
        var sortOrder by string()
    }

    /** Folders the user keeps clones in; entries that are not valid paths are left out. */
    var codeFolders: List<Path>
        get() = state.codeFolders.mapNotNull(::pathOrNull)
        set(folders) {
            state.codeFolders = folders.map(Path::toString).toMutableList()
        }

    var timing: TimingChoice
        get() = TimingChoice.restore(state.workStart, state.workEnd, state.workDays, state.nearlyOverdueHours, state.overdueHours)
        set(choice) {
            state.workStart = choice.storedStart
            state.workEnd = choice.storedEnd
            state.workDays = choice.storedWorkDays.toMutableList()
            state.nearlyOverdueHours = choice.nearlyOverdueHours
            state.overdueHours = choice.overdueHours
        }

    var sortOrder: SortOrder
        get() = SortOrderSetting.restore(state.sortOrder)
        set(order) {
            state.sortOrder = SortOrderSetting.store(order)
        }

    fun queuePreferencesIn(zone: ZoneId): QueuePreferences =
        QueuePreferences(timing = timing.toReviewTiming(zone), sortOrder = sortOrder)
}
