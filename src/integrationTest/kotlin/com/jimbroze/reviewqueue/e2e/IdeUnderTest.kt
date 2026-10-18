package com.jimbroze.reviewqueue.e2e

import com.intellij.ide.starter.ide.installer.ExistingIdeInstaller
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.tools.ide.starter.product.webstorm.WebStorm
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * The WebStorm the tests launch, chosen by the `reviewQueue.e2e.ide` system property:
 * a release version such as `2026.2.3`, `eap` for the latest EAP, or the absolute path of an installed IDE.
 */
object IdeUnderTest {
    private const val LATEST_EAP = "eap"
    private val choice: String = System.getProperty("reviewQueue.e2e.ide")

    fun opening(project: Path): TestCase<LocalProjectInfo> {
        val testCase = TestCase(IdeInfo.WebStorm, LocalProjectInfo(project))
        return when {
            choice == LATEST_EAP -> testCase.useEAP()
            Path(choice).isAbsolute -> testCase.onIDE(installedAt(Path(choice)))
            else -> testCase.withVersion(choice)
        }
    }

    private fun installedAt(path: Path): IdeInfo = IdeInfo.WebStorm.copy(getInstaller = { ExistingIdeInstaller(path) })
}
