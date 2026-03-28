package com.turnit.app
import com.turnit.app.models.ModelOption

import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RequestController(
    private val scope: CoroutineScope,
    private val geminiKey: String,
    private val hfKey: String
) {
    companion object {
        const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val HF_ROUTER = "https://router.huggingface.co/v1/chat/completions"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS) // Increased for Qwen 397B
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun send(prompt: String, model: ModelOption, imageUrl: String? = null, onResult: (String) -> Unit, onError: (String) -> Unit) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    if (model.apiType == ModelOption.TYPE_GEMINI) {
                        callGemini(prompt, model.modelId)
                    } else {
                        callHuggingFaceNovita(prompt, model.modelId, imageUrl)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                result.fold(onSuccess = { onResult(it) }, onFailure = { onError(it.message ?: "Error") })
            }
        }
    }

    private fun callGemini(prompt: String, id: String): String {
        val body = JSONObject().put("contents", JSONArray().put(
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )).toString()
        
        val req = Request.Builder().url("$GEMINI_URL/$id:generateContent?key=$geminiKey")
            .post(body.toRequestBody("application/json".toMediaType())).build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("Gemini Error: ${resp.code}")
            return JSONObject(raw).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        }
    }

    private fun callHuggingFaceNovita(prompt: String, modelId: String, imageUrl: String?): String {
        val content = JSONArray()
        content.put(JSONObject().put("type", "text").put("text", prompt))
        
        // Add Vision support if image URL is provided
        imageUrl?.let {
            content.put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", it)))
        }

        val messages = JSONArray().put(JSONObject().put("role", "user").put("content", if (imageUrl == null) prompt else content))
        
        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
        }.toString()

        val req = Request.Builder().url(HF_ROUTER)
            .addHeader("Authorization", "Bearer $hfKey")
            .post(body.toRequestBody("application/json".toMediaType())).build()

        client.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw Exception("HF Error: ${resp.code}")
            return JSONObject(raw).getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }
    }
}
