package com.turnit.app

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatResult(
    val text: String,
    val latencyMs: Long,
    val modelId: String
)

class RateLimiter(private val maxRpm: Int) {
    private val windowMs = 60_000L
    private val stamps = ArrayDeque<Long>()

    suspend fun acquire() {
        if (maxRpm == Int.MAX_VALUE) return
        val now = System.currentTimeMillis()
        while (stamps.isNotEmpty() && now - stamps.first() >= windowMs) {
            stamps.removeFirst()
        }
        if (stamps.size >= maxRpm) {
            val waitTime = windowMs - (now - stamps.first()) + 200L
            delay(waitTime)
        }
        stamps.addLast(System.currentTimeMillis())
    }
}

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String,
    private val hfKey: String
) {
    companion object {
        // Switched to v1 (Stable) to avoid the v1beta 404 errors
        const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1/models"
        const val HF_BASE = "https://api-inference.huggingface.co/models"
    }

    private val jt = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var activeJob: Job? = null
    private var userGeminiKey: String? = null
    private var userHfKey: String? = null
    
    private val geminiLimiter = RateLimiter(10)
    private val hfLimiter = RateLimiter(10)
    private val noLimiter = RateLimiter(Int.MAX_VALUE)

    fun setUserGeminiKey(k: String?) { userGeminiKey = k?.trim()?.ifEmpty { null } }
    fun setUserHfKey(k: String?) { userHfKey = k?.trim()?.ifEmpty { null } }
    fun isActive() = activeJob?.isActive == true
    fun cancel() { activeJob?.cancel(); activeJob = null }

    fun send(
        prompt: String,
        model: ModelOption,
        onResult: (ChatResult) -> Unit,
        onError: (String) -> Unit
    ) {
        activeJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val t0 = System.currentTimeMillis()
                    val text = when (model.apiType) {
                        ModelOption.TYPE_GEMINI -> {
                            val key = userGeminiKey ?: geminiKey
                            val lim = if (userGeminiKey != null) noLimiter else geminiLimiter
                            lim.acquire()
                            // Ensure modelId is mapped correctly (e.g. "gemini-1.5-flash")
                            callGemini(prompt, model.modelId, key)
                        }
                        ModelOption.TYPE_HUGGINGFACE -> {
                            val key = userHfKey ?: hfKey
                            val lim = if (userHfKey != null) noLimiter else hfLimiter
                            lim.acquire()
                            callHuggingFace(prompt, model.modelId, key)
                        }
                        else -> throw IllegalArgumentException("Unknown apiType: ${model.apiType}")
                    }
                    ChatResult(text, System.currentTimeMillis() - t0, model.modelId)
                }
            }
            if (!isActive) return@launch
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { 
                    Log.e("RequestController", "Error: ${it.message}")
                    onError(it.message ?: "Request failed") 
                }
            )
        }
    }

    private suspend fun callGemini(prompt: String, modelId: String, apiKey: String): String {
        // Sanitize modelId: Remove "models/" if it was prepended twice
        val cleanId = modelId.removePrefix("models/")
        
        val body = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            )).toString()

        val url = "$GEMINI_BASE/$cleanId:generateContent?key=$apiKey"
        val req = Request.Builder()
            .url(url)
            .post(body.toRequestBody(jt))
            .build()

        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val errJson = runCatching { JSONObject(raw).getJSONObject("error").getString("message") }.getOrNull()
                throw RuntimeException("Gemini ${resp.code}: ${errJson ?: "Unknown Error"}")
            }
            JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text").trim()
        }
    }

    private suspend fun callHuggingFace(prompt: String, modelId: String, token: String): String {
        val body = JSONObject()
            .put("inputs", prompt)
            .put("parameters", JSONObject()
                .put("max_new_tokens", 512)
                .put("return_full_text", false)
                .put("temperature", 0.7)
            ).toString()

        val req = Request.Builder()
            .url("$HF_BASE/$modelId")
            .addHeader("Authorization", "Bearer $token")
            .post(body.toRequestBody(jt))
            .build()

        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw RuntimeException("HF ${resp.code}: Model might be loading or path is wrong.")
            }
            val array = JSONArray(raw)
            array.getJSONObject(0).getString("generated_text").trim()
        }
    }

    fun close() = http.connectionPool.evictAll()
}
