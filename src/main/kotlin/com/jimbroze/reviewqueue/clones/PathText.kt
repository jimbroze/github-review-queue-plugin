package com.jimbroze.reviewqueue.clones

import java.nio.file.InvalidPathException
import java.nio.file.Path

fun pathOrNull(text: String): Path? =
    try {
        Path.of(text.trim())
    } catch (invalid: InvalidPathException) {
        null
    }
