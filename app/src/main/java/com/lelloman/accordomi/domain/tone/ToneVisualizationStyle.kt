package com.lelloman.accordomi.domain.tone

enum class ToneVisualizationStyle(
    val storageKey: String,
    val displayName: String,
) {
    Text(
        storageKey = "text",
        displayName = "Text",
    ),
    Needle(
        storageKey = "needle",
        displayName = "Needle",
    ),
    SideWheel(
        storageKey = "side_wheel",
        displayName = "Side wheel",
    );

    companion object {
        val Default = Text

        fun fromStorageKey(storageKey: String?): ToneVisualizationStyle =
            entries.firstOrNull { it.storageKey == storageKey } ?: Default
    }
}

