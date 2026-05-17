package com.lelloman.accordomi.domain.tone

enum class ToneDetectionMethod(
    val storageKey: String,
    val displayName: String,
) {
    Yin(
        storageKey = "yin",
        displayName = "YIN",
    ),
    AutoCorrelation(
        storageKey = "autocorrelation",
        displayName = "Autocorrelation",
    ),
    McLeod(
        storageKey = "mcleod",
        displayName = "McLeod",
    );

    companion object {
        val Default = Yin

        fun fromStorageKey(storageKey: String?): ToneDetectionMethod =
            entries.firstOrNull { it.storageKey == storageKey } ?: Default
    }
}
