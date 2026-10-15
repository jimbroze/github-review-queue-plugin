package io.github.jimdickinson.reviewqueue.clones

import java.nio.file.Path

object CodeFolders {
    /**
     * The folders to scan for clones: [userFolders] first, then the folders that hold [recentProjects],
     * except [home] and the file system root. Each folder appears once.
     */
    fun toSearch(userFolders: List<Path>, recentProjects: List<Path>, home: Path): List<Path> = TODO()
}
