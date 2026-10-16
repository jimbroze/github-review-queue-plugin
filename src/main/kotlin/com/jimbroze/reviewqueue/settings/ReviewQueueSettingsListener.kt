package com.jimbroze.reviewqueue.settings

import com.intellij.util.messages.Topic

fun interface ReviewQueueSettingsListener {
    fun settingsChanged()

    companion object {
        @Topic.AppLevel
        val TOPIC = Topic(ReviewQueueSettingsListener::class.java, Topic.BroadcastDirection.NONE)
    }
}
