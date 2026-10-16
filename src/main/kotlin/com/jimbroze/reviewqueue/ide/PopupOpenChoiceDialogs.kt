package com.jimbroze.reviewqueue.ide

import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueBundle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

class PopupOpenChoiceDialogs(private val project: Project) : OpenChoiceDialogs {
    override suspend fun pick(choices: List<OpenChoice>): OpenChoice? = withContext(Dispatchers.EDT) {
        val picked = CompletableDeferred<OpenChoice?>()
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(choices)
            .setTitle(ReviewQueueBundle.message("open.title"))
            .setRenderer(textListCellRenderer { choice: OpenChoice? -> choice?.let(::textFor) })
            .setItemChosenCallback { picked.complete(it) }
            .addListener(object : JBPopupListener {
                override fun onClosed(event: LightweightWindowEvent) {
                    if (!event.isOk) picked.complete(null)
                }
            })
            .createPopup()
            .showCenteredInCurrentWindow(project)
        picked.await()
    }

    override suspend fun pickCloneParentFolder(suggested: Path): Path? = withContext(Dispatchers.EDT) {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle(ReviewQueueBundle.message("clone.chooseFolder.title"))
        val start = LocalFileSystem.getInstance().findFileByNioFile(suggested)
        FileChooser.chooseFile(descriptor, project, start)?.toNioPath()
    }

    private fun textFor(choice: OpenChoice): String = when (choice) {
        is OpenChoice.OpenClone -> ReviewQueueBundle.message("open.clone", displayPath(choice.folder))
        is OpenChoice.Clone -> ReviewQueueBundle.message(
            "open.cloneInto",
            "${choice.repository.owner}/${choice.repository.name}",
            displayPath(choice.parentFolder),
        )
        OpenChoice.OpenInBrowser -> ReviewQueueBundle.message("open.browser")
    }

    private fun displayPath(folder: Path): String = FileUtil.getLocationRelativeToUserHome(folder.toString())
}
