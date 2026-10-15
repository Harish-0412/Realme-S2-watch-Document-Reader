package com.watchdoc.prototype.pagination

import org.junit.Assert.*
import org.junit.Test

class PresetPayloadGeneratorTest {
    @Test fun exactAsciiLengthsAndRecognizableEnds() {
        PresetPayloadGenerator.sizes.forEach { size ->
            val body = PresetPayloadGenerator.generate(size)
            assertEquals(size, body.length)
            assertEquals(size, body.codePointLength())
            assertTrue(body.all { it.code in 32..126 })
            assertTrue(body.startsWith("[01]"))
            assertTrue(body.contains("[02]"))
            assertTrue(body.endsWith(PresetPayloadGenerator.endMarker(size)))
            assertEquals(body, PresetPayloadGenerator.generate(size))
        }
    }
    @Test fun segmentsAreAtKnownPositions() {
        val body = PresetPayloadGenerator.generate(1000)
        (1..39).forEach { n -> assertTrue(body.substring((n - 1) * 25).startsWith("[${n.toString().padStart(2, '0')}]")) }
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNonPresetSize() { PresetPayloadGenerator.generate(101) }
}
