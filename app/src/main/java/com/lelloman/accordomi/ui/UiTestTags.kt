package com.lelloman.accordomi.ui

object UiTestTags {
    const val SettingsContent = "settings_content"
    const val ReferencePitch = "reference_pitch"
    const val OpenAppPermissions = "open_app_permissions"
    const val AllowMicrophone = "allow_microphone"
    const val RetryDetection = "retry_detection"
    const val DetectionContent = "detection_content"
    const val ToneVisualization = "tone_visualization"
    const val ProcessingLag = "processing_lag"
    const val CreateCustomTheme = "create_custom_theme"
    const val EditCustomTheme = "edit_custom_theme"

    fun detectionMethod(storageKey: String) = "detection_method_$storageKey"

    fun detectionRate(storageKey: String) = "detection_rate_$storageKey"

    fun visualizationStyle(storageKey: String) = "visualization_style_$storageKey"
}
