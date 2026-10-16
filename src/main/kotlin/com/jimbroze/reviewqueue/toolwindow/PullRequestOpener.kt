package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.queue.PullRequestRef

fun interface PullRequestOpener {
    fun open(pullRequest: PullRequestRef, url: String)
}
