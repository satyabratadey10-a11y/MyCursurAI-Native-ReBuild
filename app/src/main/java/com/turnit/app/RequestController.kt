package com.turnit.app

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatResult(val text: String, val latencyMs: Long, val modelId: String)

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String, // BuildConfig.GEMINI_API_KEY
    private val hfKey: String
) {
    companion object {
        // v1beta supports the gemini-3-flash-preview model
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val jt = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var activeJob: Job? = null

    fun send(prompt: String, model: ModelOption, onResult: (ChatResult) -> Unit, onError: (String) -> Unit) {
        activeJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val t0 = System.currentTimeMillis()
                    // Implementation of your requested Java-to-Kt logic
                    val text = callGemini(prompt, model.modelId, geminiKey)
                    ChatResult(text, System.currentTimeMillis() - t0, model.modelId)
                }
            }
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { onError(it.message ?: "Check your NEW API Key") }
            )
        }
    }

    private suspend fun callGemini(prompt: String, modelId: String, apiKey: String): String {
        val body = JSONObject().put("contents", JSONArray().put(
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )).toString()

        val url = "$BASE_URL/$modelId:generateContent?key=$apiKey"
        val req = Request.Builder().url(url).post(body.toRequestBody(jt)).build()

        return http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val msg = runCatching { JSONObject(raw).getJSONObject("error").getString("message") }.getOrNull()
                throw RuntimeException("Gemini Error: ${msg ?: resp.code}")
            }
            JSONObject(raw).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
    }

    fun close() = http.connectionPool.evictAll()
}
