package io.github.jimdickinson.reviewqueue.clones

import io.github.jimdickinson.reviewqueue.queue.RepositoryName

/** The host names that reach one GitHub server: the server's own host plus its SSH host aliases. */
class GitHubHosts private constructor(private val hosts: Set<String>) {
    operator fun contains(host: String): Boolean = host.lowercase() in hosts

    companion object {
        private const val GITHUB_COM = "github.com"

        /**
         * [aliasesSetting] is comma-separated; each entry is `alias=server`, or a bare alias of github.com.
         */
        fun of(serverHost: String, aliasesSetting: String): GitHubHosts {
            val server = serverHost.lowercase()
            val aliases = aliasesSetting.split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .map(::aliasAndServer)
                .filter { (_, aliasServer) -> aliasServer == server }
                .map { (alias, _) -> alias }
            return GitHubHosts(aliases.toSet() + server)
        }

        private fun aliasAndServer(entry: String): Pair<String, String> {
            val alias = entry.substringBefore('=').trim()
            val server = if ('=' in entry) entry.substringAfter('=').trim() else GITHUB_COM
            return alias to server
        }
    }
}

class GitHubRepository(val name: RepositoryName, private val hosts: GitHubHosts) {
    fun isClonedFrom(remoteUrl: String): Boolean {
        val remote = GitRemote.parse(remoteUrl) ?: return false
        return remote.host in hosts &&
            remote.repository.owner.equals(name.owner, ignoreCase = true) &&
            remote.repository.name.equals(name.name, ignoreCase = true)
    }
}
