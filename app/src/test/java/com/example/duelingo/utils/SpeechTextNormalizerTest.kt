package com.example.duelingo.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTextNormalizerTest {
    @Test fun `word and digit forms are identical`() {
        assertEquals(
            SpeechTextNormalizer.normalize("I have six books."),
            SpeechTextNormalizer.normalize("I have 6 books")
        )
    }

    @Test fun `compound numbers are normalized`() {
        assertEquals("we counted 21 birds", SpeechTextNormalizer.normalize("We counted twenty-one birds"))
    }
}
