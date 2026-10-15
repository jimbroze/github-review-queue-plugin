package io.github.jimdickinson.reviewqueue.clones

import io.github.jimdickinson.reviewqueue.queue.RepositoryName

/** The host names that reach one GitHub server: the server's own host plus its SSH host aliases. */
class GitHubHosts private constructor(private val hosts: Set<String>) {
    operator fun contains(host: String): Boolean = TODO()

    companion object {
        /**
         * [aliasesSetting] is comma-separated; each entry is `alias=server`, or a bare alias of github.com.
         */
        fun of(serverHost: String, aliasesSetting: String): GitHubHosts = TODO()
    }
}

class GitHubRepository(val name: RepositoryName, private val hosts: GitHubHosts) {
    fun isClonedFrom(remoteUrl: String): Boolean = TODO()
}
