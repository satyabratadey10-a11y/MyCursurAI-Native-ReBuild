package com.turnit.app

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

// ---- Result carrier ---------------------------------------------------

data class ChatResult(
    val text:      String,
    val latencyMs: Long,
    val modelId:   String
)

// ---- Suspending token-bucket rate limiter -----------------------------

class RateLimiter(private val maxRpm: Int) {
    private val windowMs  = 60_000L
    private val stamps    = ArrayDeque<Long>(maxRpm.coerceAtMost(200))
    suspend fun acquire() {
        if (maxRpm == Int.MAX_VALUE) return
        val now = System.currentTimeMillis()
        while (stamps.isNotEmpty() && now - stamps.first() >= windowMs)
            stamps.removeFirst()
        if (stamps.size >= maxRpm)
            delay(windowMs - (now - stamps.first()) + 200L)
        stamps.addLast(System.currentTimeMillis())
    }
}

// ---- RequestController -----------------------------------------------
//
// ALL requests go DIRECTLY to the official API endpoints.
// No gateway, no proxy, no placeholder URLs.
//
// Built-in keys (via BuildConfig) are rate-limited at 10 RPM.
// User-supplied keys bypass the limiter completely.

class RequestController(
    private val scope:     CoroutineScope,
    private val geminiKey: String,       // BuildConfig.GEMINI_API_KEY
    private val hfKey:     String        // BuildConfig.HUGGINGFACE_API_KEY
) {

    companion object {
        // Official Google AI Studio / Gemini REST endpoint
        // https://ai.google.dev/api/generate-content
        const val GEMINI_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models"

        // Official Hugging Face Inference API
        // https://huggingface.co/docs/api-inference
        const val HF_BASE =
            "https://api-inference.huggingface.co/models"
    }

    private val jt   = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private var activeJob:      Job?    = null
    private var userGeminiKey:  String? = null
    private var userHfKey:      String? = null

    private val geminiLimiter = RateLimiter(10)
    private val hfLimiter     = RateLimiter(10)
    private val noLimiter     = RateLimiter(Int.MAX_VALUE)

    // ---- Public API ---------------------------------------------------

    fun setUserGeminiKey(k: String?) {
        userGeminiKey = k?.trim()?.ifEmpty { null }
    }
    fun setUserHfKey(k: String?) {
        userHfKey = k?.trim()?.ifEmpty { null }
    }
    fun isActive() = activeJob?.isActive == true
    fun cancel()   { activeJob?.cancel(); activeJob = null }

    // ---- Send ---------------------------------------------------------

    fun send(
        prompt:   String,
        model:    ModelOption,
        onResult: (ChatResult) -> Unit,
        onError:  (String) -> Unit
    ) {
        activeJob = scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val t0   = System.currentTimeMillis()
                    val text = when (model.apiType) {

                        ModelOption.TYPE_GEMINI -> {
                            val key = userGeminiKey ?: geminiKey
                            val lim = if (userGeminiKey != null) noLimiter
                                      else geminiLimiter
                            lim.acquire()
                            callGemini(prompt, model.modelId, key)
                        }

                        ModelOption.TYPE_HUGGINGFACE -> {
                            val key = userHfKey ?: hfKey
                            val lim = if (userHfKey != null) noLimiter
                                      else hfLimiter
                            lim.acquire()
                            callHuggingFace(prompt, model.modelId, key)
                        }

                        else -> throw IllegalArgumentException(
                            "Unknown apiType: ${model.apiType}")
                    }
                    ChatResult(
                        text      = text,
                        latencyMs = System.currentTimeMillis() - t0,
                        modelId   = model.modelId
                    )
                }
            }
            if (!isActive) return@launch
            result.fold(
                onSuccess = { onResult(it) },
                onFailure = { onError(it.message ?: "Request failed") }
            )
        }
    }

    // ---- Gemini REST (direct) -----------------------------------------
    //
    // POST {GEMINI_BASE}/{model}:generateContent?key={GEMINI_API_KEY}
    // Content-Type: application/json
    // Body: { "contents": [{ "parts": [{ "text": "..." }] }] }

    private suspend fun callGemini(
        prompt:  String,
        modelId: String,
        apiKey:  String
    ): String {
        val body = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            )).toString()

        val req = Request.Builder()
            .url("$GEMINI_BASE/$modelId:generateContent?key=$apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(jt))
            .build()

        val raw = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException(
                "Gemini ${resp.code}: ${resp.body?.string()?.take(400)}")
            resp.body!!.string()
        }
        return JSONObject(raw)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
    }

    // ---- HuggingFace Inference API (direct) ---------------------------
    //
    // POST {HF_BASE}/{MODEL_ID}
    // Authorization: Bearer {HUGGINGFACE_API_KEY}
    // Body: { "inputs": "...", "parameters": { ... } }
    // Response: [{ "generated_text": "..." }]

    private suspend fun callHuggingFace(
        prompt:  String,
        modelId: String,
        token:   String
    ): String {
        val body = JSONObject()
            .put("inputs", prompt)
            .put("parameters", JSONObject()
                .put("max_new_tokens",  512)
                .put("return_full_text", false)
                .put("temperature",      0.7)
            ).toString()

        val req = Request.Builder()
            .url("$HF_BASE/$modelId")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type",  "application/json")
            .post(body.toRequestBody(jt))
            .build()

        val raw = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException(
                "HuggingFace ${resp.code}: ${resp.body?.string()?.take(400)}")
            resp.body!!.string()
        }
        return JSONArray(raw)
            .getJSONObject(0)
            .getString("generated_text")
            .trim()
    }

    fun close() = http.connectionPool.evictAll()
}
