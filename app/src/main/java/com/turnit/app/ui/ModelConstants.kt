package com.turnit.app.ui
import com.turnit.app.models.ModelOption

val QX_MODELS = listOf(
    // Google Stack
    ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Google - Ultra Rapid", ModelOption.TYPE_GEMINI, "G3F"),
    ModelOption("Gemini 2.5 Fast", "gemini-2.5-fast", "Google - Balanced", ModelOption.TYPE_GEMINI, "G2F"),
    ModelOption("Gemini 1.5 Pro", "gemini-1.5-pro", "Google - Complex Reasoning", ModelOption.TYPE_GEMINI, "G1P"),
    
    // Novita / HuggingFace Router Stack
    ModelOption("Qwen 3.5 72B", "qwen-3.5-72b-instruct", "Alibaba - Logic Expert", ModelOption.TYPE_HUGGINGFACE, "Q35"),
    ModelOption("DeepSeek V3", "deepseek-v3", "DeepSeek - Code Specialist", ModelOption.TYPE_HUGGINGFACE, "DSV"),
    ModelOption("Llama 3.3 70B", "llama-3.3-70b-spec", "Meta - Creative Writing", ModelOption.TYPE_HUGGINGFACE, "L33")
)
