package com.jimbroze.reviewqueue.toolwindow

import com.jimbroze.reviewqueue.queue.PullRequestRef
import com.jimbroze.reviewqueue.queue.RepositoryName
import com.jimbroze.reviewqueue.worktime.Urgency
import org.junit.Assert.assertEquals
import org.junit.Test

class PullRequestOpenerTest {

    @Test
    fun `opening a row opens that pull request`() {
        val opened = mutableListOf<Pair<PullRequestRef, String>>()
        val opener = PullRequestOpener { pullRequest, url -> opened += pullRequest to url }
        val pullRequest = PullRequestRef(RepositoryName("acme", "widgets"), 7)

        opener.open(rowFor(pullRequest, url = "https://github.com/acme/widgets/pull/7"))

        assertEquals(listOf(pullRequest to "https://github.com/acme/widgets/pull/7"), opened)
    }

    private fun rowFor(pullRequest: PullRequestRef, url: String) = ReviewRow(
        pullRequestLabel = "acme/widgets #7",
        title = "Add widgets",
        authorLogin = "octocat",
        waitingFor = "1 hour",
        urgency = Urgency.OnTime,
        requestedFrom = "you",
        isReReview = false,
        isDraft = false,
        pullRequest = pullRequest,
        url = url,
    )
}
