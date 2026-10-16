package com.jimbroze.reviewqueue.ide

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jimbroze.reviewqueue.toolwindow.BrowserPullRequestOpener
import kotlin.time.Duration.Companion.seconds

private val waitForOpenProject = 3.seconds

/** Opens pull requests from [currentProject]'s Review Queue: in an open project, a local clone, or the browser. */
fun ideFirstPullRequestOpener(currentProject: Project): IdeFirstPullRequestOpener<Project> =
    IdeFirstPullRequestOpener(
        currentProject = currentProject,
        openProjects = { ProjectManager.getInstance().openProjects.toList() },
        hasGitRemotes = ::hasGitRemotes,
        viewer = GitHubPluginPullRequestViewer(waitForOpenProject),
        bringToFront = { ApplicationManager.getApplication().invokeLater { ProjectUtil.focusProjectWindow(it, true) } },
        browser = BrowserPullRequestOpener,
        openOutsideOpenProjects = outsideOpenProjectsOpener(currentProject)::open,
    )

private fun outsideOpenProjectsOpener(currentProject: Project): OutsideOpenProjectsOpener<Project> =
    OutsideOpenProjectsOpener(
        findClones = IdeLocalClones::of,
        suggestedCloneParentFolder = IdeLocalClones::suggestedCloneParentFolder,
        dialogs = PopupOpenChoiceDialogs(currentProject),
        folders = IdeLocalFolders(currentProject),
        showWhenReady = ::showWhenProjectIsReady,
        browser = BrowserPullRequestOpener,
        reportFailure = { reportOpenFailure(currentProject, it) },
    )
