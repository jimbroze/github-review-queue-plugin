package com.jimbroze.reviewqueue.e2e

import com.intellij.driver.client.Driver
import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.sdk.VirtualFile
import com.intellij.driver.sdk.singleProject

private const val PLUGIN_ID = "com.jimbroze.reviewqueue"

/** Opens [pullRequest] the way a double-click on its Review Queue row does. */
fun Driver.openFromReviewQueue(pullRequest: TestPullRequest) = withContext {
    val repository = new(RepositoryName::class, pullRequest.repository.owner, pullRequest.repository.name)
    val row = new(
        ReviewRow::class,
        "${pullRequest.repository} #${pullRequest.number}",
        "End-to-end test pull request",
        "review-queue-e2e",
        "1 day",
        "you",
        false,
        false,
        new(PullRequestRef::class, repository, pullRequest.number),
        pullRequest.url,
    )
    service<ReviewQueueService>(singleProject()).open(row)
}

/** The web addresses of the GitHub pull request timelines open in the project's editor tabs. */
fun Driver.openPullRequestTimelines(): List<String> = withContext {
    service<OpenEditors>(singleProject()).getOpenFiles()
        .filter { it.getName().startsWith("#") }
        .map { cast(it, PathWrapper::class).getPresentablePath() }
}

fun Driver.hasGitRepository(): Boolean =
    service<GitRepositoryManager>(singleProject()).getRepositories().isNotEmpty()

fun String.isTimelineOf(pullRequest: TestPullRequest): Boolean =
    startsWith("${pullRequest.repository.url}/pull", ignoreCase = true) && endsWith("/${pullRequest.number}")

@Remote("com.jimbroze.reviewqueue.toolwindow.ReviewQueueService", plugin = PLUGIN_ID)
interface ReviewQueueService {
    fun open(row: ReviewRow)
}

@Remote("com.jimbroze.reviewqueue.toolwindow.ReviewRow", plugin = PLUGIN_ID)
interface ReviewRow

@Remote("com.jimbroze.reviewqueue.queue.PullRequestRef", plugin = PLUGIN_ID)
interface PullRequestRef

@Remote("com.jimbroze.reviewqueue.queue.RepositoryName", plugin = PLUGIN_ID)
interface RepositoryName

@Remote("com.intellij.openapi.fileEditor.FileEditorManager")
interface OpenEditors {
    fun getOpenFiles(): Array<VirtualFile>
}

/** The GitHub plugin's timeline files present the pull request's web address as their path. */
@Remote("com.intellij.openapi.vfs.VirtualFilePathWrapper")
interface PathWrapper {
    fun getPresentablePath(): String
}

@Remote("git4idea.repo.GitRepositoryManager", plugin = "Git4Idea")
interface GitRepositoryManager {
    fun getRepositories(): List<GitRepository>
}

@Remote("git4idea.repo.GitRepository", plugin = "Git4Idea")
interface GitRepository
