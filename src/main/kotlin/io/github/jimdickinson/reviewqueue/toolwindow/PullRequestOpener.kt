package io.github.jimdickinson.reviewqueue.toolwindow

import io.github.jimdickinson.reviewqueue.queue.PullRequestRef

fun interface PullRequestOpener {
    fun open(pullRequest: PullRequestRef, url: String)
}
