package io.github.jimdickinson.reviewqueue.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import io.github.jimdickinson.reviewqueue.clones.pathOrNull
import io.github.jimdickinson.reviewqueue.toolwindow.ReviewQueueBundle
import java.nio.file.Path

class ReviewQueueConfigurable : BoundConfigurable(ReviewQueueBundle.message("settings.displayName")) {
    private val settings = service<ReviewQueueSettings>()

    override fun createPanel(): DialogPanel = panel {
        row {
            textArea()
                .label(ReviewQueueBundle.message("settings.codeFolders"), LabelPosition.TOP)
                .comment(ReviewQueueBundle.message("settings.codeFolders.comment"))
                .rows(6)
                .align(Align.FILL)
                .bindText({ settings.codeFolders.joinToString("\n") }, { settings.codeFolders = foldersIn(it) })
        }
    }

    private fun foldersIn(text: String): List<Path> =
        text.lines().filter(String::isNotBlank).mapNotNull(::pathOrNull)
}
