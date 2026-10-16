package com.jimbroze.reviewqueue.e2e

import org.junit.jupiter.api.Assumptions.abort

data class GitHubRepository(val owner: String, val name: String) {
    val url: String get() = "https://github.com/$owner/$name"

    override fun toString() = "$owner/$name"

    companion object {
        fun parse(ownerSlashName: String): GitHubRepository {
            val parts = ownerSlashName.split("/")
            require(parts.size == 2 && parts.none(String::isBlank)) { "Expected owner/name, got '$ownerSlashName'" }
            return GitHubRepository(parts[0], parts[1])
        }
    }
}

data class TestPullRequest(val repository: GitHubRepository, val number: Long) {
    val url: String get() = "${repository.url}/pull/$number"
}

/**
 * The GitHub test account and repositories, read from environment variables.
 *
 * Reading a missing variable aborts the calling test, so JUnit reports it as skipped rather than failed.
 */
object E2eSettings {
    val token: String get() = required("REVIEW_QUEUE_E2E_GITHUB_TOKEN")
    val repository: GitHubRepository get() = GitHubRepository.parse(required("REVIEW_QUEUE_E2E_REPO"))
    val pullRequest: TestPullRequest get() = TestPullRequest(repository, required("REVIEW_QUEUE_E2E_PR").toLong())
    val fork: GitHubRepository get() = GitHubRepository.parse(required("REVIEW_QUEUE_E2E_FORK"))
    val ideLicense: String? get() = System.getenv("REVIEW_QUEUE_E2E_IDE_LICENSE")

    private fun required(name: String): String =
        System.getenv(name)?.takeIf(String::isNotBlank)
            ?: abort("Set $name to run the end-to-end tests; see .github/workflows/e2e-nightly.yml for all variables")
}
