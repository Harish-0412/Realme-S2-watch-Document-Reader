package com.watchdoc.prototype.pagination

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class TextChunkerTest {
    private val chunker = TextChunker()
    private fun verify(source: String, budget: Int): List<TextSection> {
        val sections = chunker.chunk(source, budget)
        assertEquals(chunker.normalize(source), sections.joinToString("") { it.text })
        sections.forEachIndexed { i, section ->
            assertFalse(section.text.isEmpty())
            assertEquals(i, section.index)
            assertEquals(sections.size, section.total)
            assertTrue("${section.body.codePointLength()} > $budget", section.body.codePointLength() <= budget)
            assertTrue(section.body.startsWith("[${i + 1}/${sections.size}] "))
        }
        return sections
    }
    @Test fun emptyAndBlankInput() {
        listOf("", "\r\n \t\r").forEach { assertTrue(chunker.chunk(it, 200).isEmpty()) }
    }
    @Test fun sourceOrderAndPunctuationSurvive() { verify("First, second! Third? (Fourth.) ".repeat(90), 73) }
    @Test fun longUnbrokenWordsUseSafeFallback() { verify("x".repeat(2345), 50) }
    @Test fun normalizesMixedLineEndingsWithoutDroppingWhitespace() {
        val source = "A\r\n\r\nB\rC\nD  \t".repeat(90)
        val sections = verify(source, 90)
        assertFalse(sections.any { '\r' in it.text })
    }
    @Test fun reservesPrefixAsNumberingCrossesTenAndHundred() {
        val sections = verify("x".repeat(20000), 50)
        assertTrue(sections.size > 100)
        assertEquals(50, sections[100].body.codePointLength())
    }
    @Test fun unicodeDoesNotSplitSurrogatePairs() {
        val sections = verify("😀🚀漢字e\u0301 العربية नमस्ते 🧑‍💻 ".repeat(150), 51)
        sections.forEach { section ->
            assertFalse(Character.isLowSurrogate(section.text.first()))
            assertFalse(Character.isHighSurrogate(section.text.last()))
            var i = 0
            while (i < section.text.length) {
                val c = section.text[i]
                if (Character.isHighSurrogate(c)) {
                    assertTrue(i + 1 < section.text.length)
                    assertTrue(Character.isLowSurrogate(section.text[i + 1]))
                    i += 2
                } else { assertFalse(Character.isLowSurrogate(c)); i++ }
            }
        }
    }
    @Test fun astralLongWordUsesCodePointsRatherThanUtf16Units() {
        val sections = verify("🚀".repeat(700), 50)
        assertTrue(sections.first().text.length > sections.first().text.codePointLength())
    }
    @Test fun prefersParagraphThenSentenceThenWordBoundaries() {
        val paragraph = "a".repeat(30) + "\n\n"
        assertEquals(paragraph, verify(paragraph + "b".repeat(60), 50).first().text)
        val sentence = "a".repeat(30) + "."
        assertEquals(sentence, verify(sentence + " " + "b".repeat(60), 50).first().text)
        val word = "a".repeat(30) + " "
        assertEquals(word, verify(word + "b".repeat(60), 50).first().text)
    }
    @Test fun whitespaceRunsAreNotSilentlyDiscarded() { verify("Start" + " \n".repeat(300) + "End\n ", 50) }
    @Test fun deterministicAcrossCalls() {
        val text = "Networks transport messages.\n\n".repeat(50)
        assertEquals(chunker.chunk(text, 200), chunker.chunk(text, 200))
    }
    @Test fun variedInputsRespectBudgetsAndPreserveOrder() {
        val random = Random(710)
        val tokens = listOf("a", "word ", ". ", "\r\n", "\n\n", "😀", "字", "e\u0301", "🚀", " ")
        repeat(120) {
            val source = "Start " + (0..random.nextInt(10, 800)).joinToString("") { tokens[random.nextInt(tokens.size)] }
            verify(source, random.nextInt(50, 1001))
        }
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidBudget() { chunker.chunk("abc", 49) }
    @Test fun navigationStaysWithinBounds() {
        assertEquals(0, ReaderPosition.previous(0, 12))
        assertEquals(11, ReaderPosition.next(11, 12))
        assertEquals(3, ReaderPosition.next(2, 12))
        assertEquals(1, ReaderPosition.previous(2, 12))
        assertEquals(0, ReaderPosition.bound(90, 0))
        assertEquals(0, ReaderPosition.next(0, 0))
        assertEquals(0, ReaderPosition.previous(0, 0))
        assertEquals(0, ReaderPosition.bound(-1, 12))
    }
}
