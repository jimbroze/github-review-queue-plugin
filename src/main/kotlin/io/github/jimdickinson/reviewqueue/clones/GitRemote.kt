package io.github.jimdickinson.reviewqueue.clones

import io.github.jimdickinson.reviewqueue.queue.RepositoryName

data class GitRemote(val host: String, val repository: RepositoryName) {
    companion object {
        /** Returns null for URLs that do not name an owner/name repository. [host] is lower case. */
        fun parse(url: String): GitRemote? = TODO()
    }
}
