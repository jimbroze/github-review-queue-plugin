package com.jimbroze.reviewqueue.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.jimbroze.reviewqueue.clones.pathOrNull
import com.jimbroze.reviewqueue.toolwindow.ReviewQueueBundle
import java.nio.file.Path

class ReviewQueueConfigurable : BoundConfigurable(ReviewQueueBundle.message("settings.displayName")) {
    private val settings = service<ReviewQueueSettings>()
    private val timingSection = TimingSection(settings)

    override fun createPanel(): DialogPanel = panel {
        timingSection.addTo(this)
        row {
            textArea()
                .label(ReviewQueueBundle.message("settings.codeFolders"), LabelPosition.TOP)
                .comment(ReviewQueueBundle.message("settings.codeFolders.comment"))
                .rows(6)
                .align(Align.FILL)
                .bindText({ settings.codeFolders.joinToString("\n") }, { settings.codeFolders = foldersIn(it) })
        }
    }

    override fun apply() {
        super.apply()
        timingSection.save()
        ApplicationManager.getApplication().messageBus.syncPublisher(ReviewQueueSettingsListener.TOPIC).settingsChanged()
    }

    private fun foldersIn(text: String): List<Path> =
        text.lines().filter(String::isNotBlank).mapNotNull(::pathOrNull)
}
