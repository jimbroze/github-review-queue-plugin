package com.jimbroze.reviewqueue.e2e

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.FakeBrowser
import com.intellij.driver.sdk.replaceIdeBrowser
import com.intellij.driver.sdk.waitFor
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.driver.sdk.waitForProjectOpen
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class OpenPullRequestInIdeTest {
    @Test
    fun `a pull request on the open project's GitHub repository opens in the IDE's pull request view`() {
        val pullRequest = E2eSettings.pullRequest
        val project = LocalClone.of(pullRequest.repository, E2eSettings.token)

        withIdeOpening(project, testName = "single-remote") { browser ->
            openFromReviewQueue(pullRequest)

            waitForTimelineOf(pullRequest)
            assertNull(browser.getLatestUrl(), "The browser should not open")
        }
    }

    @Test
    fun `a pull request on a repository no open project has opens in the browser`() {
        val project = LocalClone.of(E2eSettings.repository, E2eSettings.token)
        val elsewhere = TestPullRequest(GitHubRepository("review-queue-e2e", "not-cloned-anywhere"), 1)

        withIdeOpening(project, testName = "other-repository") { browser ->
            openFromReviewQueue(elsewhere)

            waitFor("The browser opens ${elsewhere.url}", timeout = 30.seconds) { browser.getLatestUrl() == elsewhere.url }
            assertEquals(emptyList<String>(), openPullRequestTimelines())
        }
    }

    @Test
    fun `a pull request on the upstream of a forked clone opens in the IDE's pull request view`() {
        val pullRequest = E2eSettings.pullRequest
        val project = LocalClone.of(E2eSettings.fork, E2eSettings.token).withRemote("upstream", pullRequest.repository)

        withIdeOpening(project, testName = "fork-with-upstream") { browser ->
            openFromReviewQueue(pullRequest)

            waitForTimelineOf(pullRequest)
            assertNull(browser.getLatestUrl(), "The browser should not open")
        }
    }

    private fun withIdeOpening(project: LocalClone, testName: String, test: Driver.(FakeBrowser) -> Unit) {
        val token = E2eSettings.token
        Starter.newContext(testName, IdeUnderTest.opening(project.directory)).apply {
            pluginConfigurator.installPluginFromPath(Path(System.getProperty("path.to.build.plugin")))
            applyVMOptionsPatch { addSystemProperty("idea.trust.all.projects", true) }
            setLicense(E2eSettings.ideLicense)
            addGitHubAccount()
        }.runIdeWithDriver().useDriverAndCloseIde {
            signInToGitHub(token)
            waitForProjectOpen()
            waitForIndicators(5.minutes)
            waitFor("Git finds the cloned repository", timeout = 1.minutes) { hasGitRepository() }
            test(replaceIdeBrowser())
        }
    }

    private fun Driver.waitForTimelineOf(pullRequest: TestPullRequest) {
        waitFor("The pull request view shows ${pullRequest.url}", timeout = 30.seconds) {
            openPullRequestTimelines().any { it.isTimelineOf(pullRequest) }
        }
    }
}
