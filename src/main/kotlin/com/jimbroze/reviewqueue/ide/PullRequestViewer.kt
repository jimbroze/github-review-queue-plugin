package com.jimbroze.reviewqueue.ide

import com.jimbroze.reviewqueue.queue.PullRequestRef
import com.jimbroze.reviewqueue.queue.RepositoryName

/**
 * Shows pull requests in a project's own pull request view.
 *
 * Both functions return false, rather than throwing, when the answer is unknown or showing fails.
 */
interface PullRequestViewer<P> {
    suspend fun hasRepository(project: P, repository: RepositoryName): Boolean

    suspend fun show(project: P, pullRequest: PullRequestRef): Boolean
}
