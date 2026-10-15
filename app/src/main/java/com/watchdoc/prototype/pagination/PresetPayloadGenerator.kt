package com.watchdoc.prototype.pagination

object PresetPayloadGenerator {
    val sizes = listOf(100, 250, 500, 1000)
    fun endMarker(size: Int) = "|END-${size.toString().padStart(4, '0')}|"
    fun generate(size: Int): String {
        require(size in sizes)
        val marker = endMarker(size)
        val content = buildString {
            var segment = 1
            while (length < size - marker.length) {
                append("[${segment.toString().padStart(2, '0')}] Read the next line. ".padEnd(25, ' '))
                segment++
            }
        }.take(size - marker.length)
        return content + marker
    }
}
