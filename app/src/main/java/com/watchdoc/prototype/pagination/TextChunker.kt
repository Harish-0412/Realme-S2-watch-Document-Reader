package com.watchdoc.prototype.pagination

fun String.codePointLength(): Int = codePointCount(0, length)

data class TextSection(val text: String, val index: Int, val total: Int) {
    val prefix: String get() = "[${index + 1}/$total] "
    val body: String get() = prefix + text
}

/** Limits are Unicode code points, not UTF-16 units or grapheme clusters.
 * Concatenating section.text reproduces normalized input exactly, including whitespace.
 * Prefix space is reserved by a monotonically increasing digit-width fixed point.
 */
class TextChunker {
    fun normalize(source: String): String = source.replace("\r\n", "\n").replace('\r', '\n')

    fun chunk(source: String, bodyBudget: Int): List<TextSection> {
        require(bodyBudget in 50..1000) { "Body budget must be 50–1,000 code points." }
        val normalized = normalize(source)
        if (normalized.isBlank()) return emptyList()
        var digits = 1
        while (true) {
            // [9/9] = five characters, plus one space; each added digit costs two.
            val contentBudget = bodyBudget - (4 + 2 * digits)
            require(contentBudget > 0) { "Section prefix exceeds budget." }
            val parts = split(normalized, contentBudget)
            val needed = parts.size.toString().length
            if (needed <= digits) return parts.mapIndexed { i, text -> TextSection(text, i, parts.size) }
            digits = needed
        }
    }

    private fun split(source: String, budget: Int): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        var remaining = source.codePointLength()
        while (start < source.length) {
            val hardEnd = source.offsetByCodePoints(start, minOf(remaining, budget))
            var end = hardEnd
            if (hardEnd < source.length) {
                val floor = source.offsetByCodePoints(start, maxOf(1, budget / 2))
                var paragraph = -1
                var sentence = -1
                var word = -1
                var cursor = start
                while (cursor < hardEnd) {
                    val cp = source.codePointAt(cursor)
                    val next = cursor + Character.charCount(cp)
                    if (next >= floor) {
                        if (cp == '\n'.code && cursor > start && source[cursor - 1] == '\n') paragraph = next
                        if ((cp == '.'.code || cp == '!'.code || cp == '?'.code || cp == '。'.code || cp == '！'.code || cp == '？'.code) &&
                            (next == source.length || Character.isWhitespace(source.codePointAt(next)))) sentence = next
                        if (Character.isWhitespace(cp)) word = next
                    }
                    cursor = next
                }
                end = when { paragraph > start -> paragraph; sentence > start -> sentence; word > start -> word; else -> hardEnd }
            }
            result += source.substring(start, end)
            remaining -= source.codePointCount(start, end)
            start = end
        }
        return result
    }
}

object ReaderPosition {
    fun bound(index: Int, count: Int): Int = if (count <= 0) 0 else index.coerceIn(0, count - 1)
    fun previous(index: Int, count: Int): Int = bound(index - 1, count)
    fun next(index: Int, count: Int): Int = bound(index + 1, count)
}
