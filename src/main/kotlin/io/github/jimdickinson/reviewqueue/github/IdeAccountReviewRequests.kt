package io.github.jimdickinson.reviewqueue.github

import com.intellij.openapi.components.service
import io.github.jimdickinson.reviewqueue.queue.ReviewRequestsResult
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import java.net.URI

/** Fetches review requests as the GitHub account currently signed in to the IDE. */
suspend fun fetchReviewRequestsForIdeAccount(): ReviewRequestsResult {
    val accountManager = service<GHAccountManager>()
    return fetchReviewRequestsFor(accountManager.accountsState.value, accountManager::findCredentials)
}

/**
 * Fetches review requests as one of [accounts], preferring a github.com account, else the first.
 * Returns [ReviewRequestsResult.NotSignedIn] when there is no account or [findToken] has no token for it.
 */
internal suspend fun fetchReviewRequestsFor(
    accounts: Collection<GithubAccount>,
    findToken: suspend (GithubAccount) -> String?,
    transportFor: (endpoint: URI, token: String) -> GitHubGraphQl = ::HttpGitHubGraphQl,
): ReviewRequestsResult {
    val account = preferredAccount(accounts) ?: return ReviewRequestsResult.NotSignedIn
    val token = findToken(account) ?: return ReviewRequestsResult.NotSignedIn
    val transport = transportFor(URI(account.server.toGraphQLUrl()), token)
    return GitHubReviewSource(transport).fetchReviewRequests()
}

/** The GitHub account signed in to the IDE that the review queue uses, or null when there is none. */
fun preferredIdeAccount(): GithubAccount? = preferredAccount(service<GHAccountManager>().accountsState.value)

private fun preferredAccount(accounts: Collection<GithubAccount>): GithubAccount? =
    accounts.firstOrNull { it.server.isGithubDotCom } ?: accounts.firstOrNull()
