package com.turnit.app

import com.turnit.app.models.ModelOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String,
    private val hfKey: String
) {
    fun send(
        prompt: String,
        model: ModelOption,
        history: List<Pair<String, String>>?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                // Simulation of API call logic - replace with your actual 2026 API implementation
                onSuccess("Response from ${model.displayName}: Logic for ${model.modelId} is active.")
            } catch (e: Exception) {
                onError(e.message ?: "Unknown Error")
            }
        }
    }
}
