package com.jimbroze.reviewqueue.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jimbroze.reviewqueue.settings.AutoRefresh
import com.jimbroze.reviewqueue.settings.ReviewQueueSettings
import com.jimbroze.reviewqueue.settings.ReviewQueueSettingsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.ZoneId

/** The one review queue every project window shows; fetches only while some window is watching it. */
@Service(Service.Level.APP)
class SharedReviewQueue(private val scope: CoroutineScope) {
    private val settings = service<ReviewQueueSettings>()
    private val watchers = QueueWatchers()

    private val viewModel = ReviewQueueViewModel(
        source = ideAccountReviewRequests,
        clock = Clock.systemUTC(),
        preferences = currentPreferences(),
    )

    private var autoRefreshing: Job? = null

    val state: StateFlow<ReviewQueueState> = viewModel.state

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { watchers.startedWatching.collect { viewModel.refresh() } }
        scope.launch { viewModel.keepWaitingTimesCurrent(ticksEvery(Duration.ofMinutes(1)).onlyWhile(watchers.isWatched)) }
        startAutoRefresh()
        ApplicationManager.getApplication().messageBus.connect(scope).subscribe(
            ReviewQueueSettingsListener.TOPIC,
            ReviewQueueSettingsListener {
                viewModel.usePreferences(currentPreferences())
                startAutoRefresh()
            },
        )
    }

    fun refresh() {
        scope.launch { viewModel.refresh() }
    }

    fun watch(): QueueWatchers.Watch = watchers.watch()

    private fun currentPreferences(): QueuePreferences = settings.queuePreferencesIn(ZoneId.systemDefault())

    private fun startAutoRefresh() {
        autoRefreshing?.cancel()
        autoRefreshing = when (val autoRefresh = settings.autoRefresh) {
            AutoRefresh.Off -> null
            is AutoRefresh.Every -> scope.launch {
                viewModel.keepRefreshing(ticksEvery(autoRefresh.interval).onlyWhile(watchers.isWatched))
            }
        }
    }
}
