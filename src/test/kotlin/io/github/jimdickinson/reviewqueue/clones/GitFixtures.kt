package io.github.jimdickinson.reviewqueue.clones

import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

fun TemporaryFolder.folder(path: String): Path = Files.createDirectories(root.toPath().resolve(path))

fun TemporaryFolder.clone(path: String, vararg remotes: Pair<String, String>): Path {
    val clone = folder(path)
    Files.createDirectories(clone.resolve(".git"))
    Files.writeString(clone.resolve(".git/config"), gitConfig(*remotes))
    return clone
}

fun gitConfig(vararg remotes: Pair<String, String>): String = buildString {
    appendLine("[core]")
    appendLine("\trepositoryformatversion = 0")
    appendLine("\tbare = false")
    for ((name, url) in remotes) {
        appendLine("[remote \"$name\"]")
        appendLine("\turl = $url")
        appendLine("\tfetch = +refs/heads/*:refs/remotes/$name/*")
    }
    appendLine("[branch \"main\"]")
    appendLine("\tremote = origin")
}
