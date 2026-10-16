package com.jimbroze.reviewqueue.toolwindow

/**
 * Splits [pieces], in order, into lines no wider than [width], with [gap] between neighbouring pieces.
 * A piece wider than [width] gets a line of its own. Once [maxLines] lines are used, the rest share the last.
 */
fun <T> wrapIntoLines(pieces: List<T>, width: Int, gap: Int, maxLines: Int, widthOf: (T) -> Int): List<List<T>> {
    val lines = mutableListOf<MutableList<T>>()
    var lineWidth = 0
    for (piece in pieces) {
        val pieceWidth = widthOf(piece)
        val current = lines.lastOrNull()
        val fits = current != null && lineWidth + gap + pieceWidth <= width
        if (current != null && (fits || lines.size == maxLines)) {
            current.add(piece)
            lineWidth += gap + pieceWidth
        } else {
            lines.add(mutableListOf(piece))
            lineWidth = pieceWidth
        }
    }
    return lines
}
