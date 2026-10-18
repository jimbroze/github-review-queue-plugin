package com.jimbroze.reviewqueue.ide

import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager

/** False while the IDE is still detecting a freshly opened project's repositories. */
fun hasGitRemotes(project: Project): Boolean =
    !project.isDisposed && GitRepositoryManager.getInstance(project).repositories.any { it.remotes.isNotEmpty() }
