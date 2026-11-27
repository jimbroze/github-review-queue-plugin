package com.jimbroze.reviewqueue.toolwindow

/** Background and text colours as 0xRRGGBB. */
data class LabelColors(val background: Int, val text: Int) {
    companion object {
        val Neutral = LabelColors(background = 0xEDEDED, text = 0x000000)
    }
}

private const val WHITE = 0xFFFFFF
private const val BLACK = 0x000000
private const val HEX_DIGITS = 6
private const val LIGHT_LUMINANCE_THRESHOLD = 0.5

fun labelColorsFor(colorHex: String): LabelColors {
    val isSixHexDigits = colorHex.length == HEX_DIGITS && colorHex.all { it.digitToIntOrNull(radix = 16) != null }
    if (!isSixHexDigits) return LabelColors.Neutral
    val background = colorHex.toInt(radix = 16)
    return LabelColors(background, text = if (perceivedLuminance(background) > LIGHT_LUMINANCE_THRESHOLD) BLACK else WHITE)
}

private fun perceivedLuminance(rgb: Int): Double {
    val red = (rgb shr 16) and 0xFF
    val green = (rgb shr 8) and 0xFF
    val blue = rgb and 0xFF
    return (0.299 * red + 0.587 * green + 0.114 * blue) / 255
}
