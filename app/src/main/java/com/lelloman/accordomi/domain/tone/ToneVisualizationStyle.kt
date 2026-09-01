package com.lelloman.accordomi.domain.tone

enum class ToneVisualizationStyle(
    val storageKey: String,
) {
    Text(
        storageKey = "text",
    ),
    Needle(
        storageKey = "needle",
    ),
    SideWheel(
        storageKey = "side_wheel",
    ),
    PianoKeyboard(
        storageKey = "piano_keyboard",
    );

    companion object {
        val Default = Text

        fun fromStorageKey(storageKey: String?): ToneVisualizationStyle =
            entries.firstOrNull { it.storageKey == storageKey } ?: Default
    }
}
