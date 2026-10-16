package com.jimbroze.reviewqueue.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.jimbroze.reviewqueue.clones.pathOrNull
import java.nio.file.Path

@Service(Service.Level.APP)
@State(name = "ReviewQueueSettings", storages = [Storage("reviewQueue.xml")])
class ReviewQueueSettings : SimplePersistentStateComponent<ReviewQueueSettings.StoredSettings>(StoredSettings()) {
    class StoredSettings : BaseState() {
        var codeFolders by list<String>()
    }

    /** Folders the user keeps clones in; entries that are not valid paths are left out. */
    var codeFolders: List<Path>
        get() = state.codeFolders.mapNotNull(::pathOrNull)
        set(folders) {
            state.codeFolders = folders.map(Path::toString).toMutableList()
        }
}
