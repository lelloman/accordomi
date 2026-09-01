package com.lelloman.accordomi.feature.settings

import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Locale

internal class ReferencePitchNumberFormatter(
    private val locale: Locale,
) {
    fun format(value: Double): String = numberFormat().format(value)

    fun parse(value: String): Double? {
        if (value.isBlank()) return null

        val position = ParsePosition(0)
        val parsed = numberFormat().parse(value, position) ?: return null
        if (position.errorIndex >= 0 || position.index != value.length) return null

        return parsed.toDouble()
    }

    private fun numberFormat(): NumberFormat = NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = false
        minimumFractionDigits = ReferencePitchFractionDigits
        maximumFractionDigits = ReferencePitchFractionDigits
    }

    private companion object {
        const val ReferencePitchFractionDigits = 1
    }
}
