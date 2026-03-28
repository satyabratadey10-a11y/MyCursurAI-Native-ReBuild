package com.turnit.app.models

/**
 * Represents a selectable AI model in TurnIt QX.
 * id          -> API model identifier string
 * displayName -> Full human-readable name shown in the selector list
 * shortLabel  -> 3-character label shown inside the 48dp circle icon
 * apiType     -> Routing constant: TYPE_GEMINI or TYPE_HUGGINGFACE
 */
data class ModelOption(
    val id:          String,
    val displayName: String,
    val shortLabel:  String,
    val apiType:     Int
) {
    companion object {
        const val TYPE_GEMINI      = 0
        const val TYPE_HUGGINGFACE = 1
    }
}

/** Canonical model list shared across the entire app. */
val QX_MODELS = listOf(
    ModelOption("gemini-3-flash",  "Gemini 3 Flash",    "G3F",
        ModelOption.TYPE_GEMINI),
    ModelOption("gemini-2.5-fast", "Gemini 2.5 Fast",   "G2F",
        ModelOption.TYPE_GEMINI),
    ModelOption("qwen-3.5-novita","Qwen 3.5 (Novita)",  "Q3N",
        ModelOption.TYPE_HUGGINGFACE)
)
