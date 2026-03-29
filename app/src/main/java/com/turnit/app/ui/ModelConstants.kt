package com.turnit.app.ui

import com.turnit.app.models.ModelOption

val QX_MODELS = listOf(
    ModelOption("Gemini 3 Flash", "gemini-3-flash-preview", "Google", ModelOption.TYPE_GEMINI, "G3F"),
    ModelOption("Gemini 2.5 Fast", "gemini-2.5-fast", "Google", ModelOption.TYPE_GEMINI, "G2F"),
    ModelOption("Qwen 3.5", "qwen-3.5-novita", "Alibaba", ModelOption.TYPE_HUGGINGFACE, "Q35"),
    ModelOption("DeepSeek V3", "deepseek-v3", "DeepSeek", ModelOption.TYPE_HUGGINGFACE, "DSV"),
    ModelOption("Llama 3.3 70B", "llama-3.3-70b-spec", "Meta", ModelOption.TYPE_HUGGINGFACE, "L33"),
    ModelOption("MiniMax-M2.5", "MiniMaxAI/MiniMax-M2.5:novita", "MiniMax", ModelOption.TYPE_HUGGINGFACE, "MMX")
)

const val MSG_USER = 0
const val MSG_AI   = 1
