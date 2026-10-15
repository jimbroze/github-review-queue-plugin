package io.github.jimdickinson.reviewqueue.e2e

import com.intellij.driver.client.Driver
import com.intellij.driver.client.Remote
import com.intellij.driver.client.service
import com.intellij.driver.client.utility
import com.intellij.ide.starter.ide.IDETestContext
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

private const val ACCOUNT_ID = "review-queue-e2e"

/**
 * Adds a github.com account, without a token, to the started IDE's own config,
 * and keeps that IDE's passwords in memory so nothing reaches the machine's keychain.
 */
fun IDETestContext.addGitHubAccount() {
    writeOptions(
        "github.xml",
        """
        <application>
          <component name="GithubAccounts">
            <account name="$ACCOUNT_ID" id="$ACCOUNT_ID">
              <server host="github.com" />
            </account>
          </component>
        </application>
        """,
    )
    writeOptions(
        "security.xml",
        """
        <application>
          <component name="PasswordSafe">
            <option name="PROVIDER" value="MEMORY_ONLY" />
          </component>
        </application>
        """,
    )
}

/** Gives the account from [addGitHubAccount] its token. Passwords are in memory only, so call this once the IDE runs. */
fun Driver.signInToGitHub(token: String) = withContext {
    val serviceName = utility<CredentialServiceNames>().generateServiceName(GITHUB_SUBSYSTEM, ACCOUNT_ID)
    service<PasswordSafe>().set(new(CredentialAttributes::class, serviceName), new(Credentials::class, ACCOUNT_ID, token))
}

private fun IDETestContext.writeOptions(fileName: String, xml: String) {
    paths.configDir.resolve("options").createDirectories().resolve(fileName).writeText(xml.trimIndent())
}

/** The GitHub plugin stores each account's token under this subsystem, keyed by account id. */
private const val GITHUB_SUBSYSTEM = "GitHub"

@Remote("com.intellij.credentialStore.CredentialAttributesKt")
interface CredentialServiceNames {
    fun generateServiceName(subsystem: String, key: String): String
}

@Remote("com.intellij.ide.passwordSafe.PasswordSafe")
interface PasswordSafe {
    fun set(attributes: CredentialAttributes, credentials: Credentials)
}

@Remote("com.intellij.credentialStore.CredentialAttributes")
interface CredentialAttributes

@Remote("com.intellij.credentialStore.Credentials")
interface Credentials
