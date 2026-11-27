package com.jimbroze.reviewqueue.toolwindow

/** Background and text colours as 0xRRGGBB. */
data class LabelColors(val background: Int, val text: Int) {
    companion object {
        val Neutral = LabelColors(background = 0xEDEDED, text = 0x000000)
    }
}

fun labelColorsFor(colorHex: String): LabelColors = TODO("derive label colours from GitHub's hex colour")
