package com.lelloman.accordomi.feature.settings

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferencePitchNumberFormatterTest {
    @Test
    fun formatsAndParsesEnglishDecimalSeparator() {
        val formatter = ReferencePitchNumberFormatter(Locale.US)

        assertEquals("440.0", formatter.format(440.0))
        assertEquals(442.5, formatter.parse("442.5")!!, 0.0)
        assertNull(formatter.parse("442,5"))
    }

    @Test
    fun formatsAndParsesItalianDecimalSeparator() {
        val formatter = ReferencePitchNumberFormatter(Locale.ITALY)

        assertEquals("440,0", formatter.format(440.0))
        assertEquals(442.5, formatter.parse("442,5")!!, 0.0)
        assertNull(formatter.parse("442.5"))
    }

    @Test
    fun rejectsBlankPartialAndGroupedInput() {
        val formatter = ReferencePitchNumberFormatter(Locale.US)

        assertNull(formatter.parse(""))
        assertNull(formatter.parse("442 Hz"))
        assertNull(formatter.parse("4,420.0"))
    }
}
