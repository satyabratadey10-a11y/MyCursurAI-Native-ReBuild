package com.turnit.app.models

data class ModelOption(
    val displayName: String,
    val modelId: String,
    val description: String,
    val apiType: Int,
    val shortLabel: String
) {
    companion object {
        const val TYPE_GEMINI = 0
        const val TYPE_HUGGINGFACE = 1
    }
}
