package com.lelloman.accordomi.domain.tone

enum class ToneDetectionMethod(
    val storageKey: String,
) {
    Yin(
        storageKey = "yin",
    ),
    AutoCorrelation(
        storageKey = "autocorrelation",
    ),
    McLeod(
        storageKey = "mcleod",
    );

    companion object {
        val Default = Yin

        fun fromStorageKey(storageKey: String?): ToneDetectionMethod =
            entries.firstOrNull { it.storageKey == storageKey } ?: Default
    }
}
