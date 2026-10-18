package com.jimbroze.reviewqueue.toolwindow

import com.intellij.ide.BrowserUtil
import com.jimbroze.reviewqueue.queue.PullRequestRef

object BrowserPullRequestOpener : PullRequestOpener {
    override fun open(pullRequest: PullRequestRef, url: String) {
        BrowserUtil.browse(url)
    }
}
