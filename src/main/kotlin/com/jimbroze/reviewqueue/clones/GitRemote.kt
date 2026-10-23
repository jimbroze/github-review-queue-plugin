package com.jimbroze.reviewqueue.clones

import com.jimbroze.reviewqueue.queue.RepositoryName

data class GitRemote(val host: String, val repository: RepositoryName) {
    companion object {
        private val urlWithScheme = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*://(?:[^@/]+@)?([^/:]+)(?::\d+)?/(.+)$""")
        private val scpStyle = Regex("""^(?:[^@/]+@)?([^/:]+):(.+)$""")

        /** Returns null for URLs that do not name an owner/name repository. [host] is lower case. */
        fun parse(url: String): GitRemote? {
            val (host, path) = (urlWithScheme.matchEntire(url) ?: scpStyle.matchEntire(url))?.destructured ?: return null
            val segments = path.trim('/').removeSuffix(".git").split('/')
            if (segments.size != 2 || segments.any { it.isEmpty() }) return null
            return GitRemote(host.lowercase(), RepositoryName(segments[0], segments[1]))
        }
    }
}
