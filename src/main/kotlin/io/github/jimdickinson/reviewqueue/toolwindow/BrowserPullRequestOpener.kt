package io.github.jimdickinson.reviewqueue.toolwindow

import com.intellij.ide.BrowserUtil
import io.github.jimdickinson.reviewqueue.queue.PullRequestRef

object BrowserPullRequestOpener : PullRequestOpener {
    override fun open(pullRequest: PullRequestRef, url: String) {
        BrowserUtil.browse(url)
    }
}
