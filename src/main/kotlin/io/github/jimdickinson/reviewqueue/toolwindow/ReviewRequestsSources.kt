package io.github.jimdickinson.reviewqueue.toolwindow

import com.intellij.openapi.project.Project
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult

@Suppress("UNUSED_PARAMETER")
fun reviewRequestsSourceFor(project: Project): ReviewRequestsSource = signedOutUntilGitHubAccountsAreConnected

private val signedOutUntilGitHubAccountsAreConnected = ReviewRequestsSource { ReviewRequestsResult.NotSignedIn }
