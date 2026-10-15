package io.github.jimdickinson.reviewqueue.e2e

import java.nio.file.Path
import java.util.Base64
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

/** A shallow clone of a GitHub repository in a new temporary directory, ready to open as a project. */
class LocalClone private constructor(val directory: Path) {
    fun withRemote(name: String, repository: GitHubRepository): LocalClone = apply {
        git(directory, "remote", "add", name, "${repository.url}.git")
    }

    companion object {
        fun of(repository: GitHubRepository, token: String): LocalClone {
            val directory = createTempDirectory("review-queue-e2e-").resolve(repository.name)
            git(directory.parent, "clone", "--depth=1", "${repository.url}.git", directory.toString(), token = token)
            declareGitRoot(directory)
            return LocalClone(directory)
        }

        /** Saves the IDE from auto-detecting the Git root after the project opens, which would race the tests. */
        private fun declareGitRoot(directory: Path) {
            directory.resolve(".idea").createDirectories().resolve("vcs.xml").writeText(
                """
                <project version="4">
                  <component name="VcsDirectoryMappings">
                    <mapping directory="${'$'}PROJECT_DIR${'$'}" vcs="Git" />
                  </component>
                </project>
                """.trimIndent(),
            )
        }

        private fun git(workingDirectory: Path, vararg arguments: String, token: String? = null) {
            val process = ProcessBuilder(listOf("git") + arguments)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .apply { if (token != null) environment().putAll(gitHubAuthorization(token)) }
                .start()
            val output = process.inputStream.bufferedReader().readText()
            check(process.waitFor() == 0) { "git ${arguments.first()} failed:\n$output" }
        }

        /** Git reads `GIT_CONFIG_*` variables as extra config, so the token is never written into the clone. */
        private fun gitHubAuthorization(token: String): Map<String, String> {
            val credentials = Base64.getEncoder().encodeToString("x-access-token:$token".toByteArray())
            return mapOf(
                "GIT_CONFIG_COUNT" to "1",
                "GIT_CONFIG_KEY_0" to "http.https://github.com/.extraheader",
                "GIT_CONFIG_VALUE_0" to "AUTHORIZATION: basic $credentials",
            )
        }
    }
}
