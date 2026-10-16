package com.jimbroze.reviewqueue.ide

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jimbroze.reviewqueue.queue.PullRequestRef
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueBundle
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val waitForNewlyOpenedProject = 30.seconds
private val gitRemotesPollInterval = 500.milliseconds

/** Shows [pullRequest] in a project that has just been opened, waiting at most 30 seconds for it to be ready. */
suspend fun showWhenProjectIsReady(project: Project, pullRequest: PullRequestRef): Boolean =
    withTimeoutOrNull(waitForNewlyOpenedProject) {
        while (!hasGitRemotes(project)) delay(gitRemotesPollInterval)
        GitHubPluginPullRequestViewer(waitForNewlyOpenedProject).show(project, pullRequest)
    } ?: false

fun reportOpenFailure(project: Project, failure: OpenFailure) {
    val message = when (failure) {
        is OpenFailure.CouldNotOpenFolder ->
            ReviewQueueBundle.message("failure.openFolder", FileUtil.getLocationRelativeToUserHome(failure.folder.toString()))
        is OpenFailure.CouldNotClone ->
            ReviewQueueBundle.message("failure.clone", "${failure.repository.owner}/${failure.repository.name}")
        is OpenFailure.CouldNotShow -> ReviewQueueBundle.message("failure.show", failure.pullRequest.number)
    }
    Notification("Review Queue", message, NotificationType.WARNING).notify(project)
}
