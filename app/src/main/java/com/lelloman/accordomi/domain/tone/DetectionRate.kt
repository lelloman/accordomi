package com.lelloman.accordomi.domain.tone

enum class DetectionRate(
    val storageKey: String,
    val audioFrameStep: Long,
) {
    Efficient(
        storageKey = "efficient",
        audioFrameStep = 4,
    ),
    Balanced(
        storageKey = "balanced",
        audioFrameStep = 2,
    ),
    High(
        storageKey = "high",
        audioFrameStep = 1,
    );

    companion object {
        val Default = Balanced

        fun fromStorageKey(storageKey: String?): DetectionRate =
            entries.firstOrNull { it.storageKey == storageKey } ?: Default
    }
}
