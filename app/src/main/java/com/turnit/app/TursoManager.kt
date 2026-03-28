package com.turnit.app

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class TursoManager(private val dbUrl: String, private val authToken: String) {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun signup(username: String, email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val id = UUID.randomUUID().toString()
        val sql = "INSERT INTO users (id, username, email, password_hash) VALUES ('$id', '$username', '$email', '$pass');"
        execute(sql) { success, _ -> onResult(success, if(success) id else "Signup Failed") }
    }

    fun login(username: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val sql = "SELECT id FROM users WHERE username = '$username' AND password_hash = '$pass';"
        execute(sql) { success, data ->
            if (success && data != null) {
                val rows = data.getJSONArray("rows")
                if (rows.length() > 0) onResult(true, rows.getJSONArray(0).getString(0))
                else onResult(false, "Invalid Credentials")
            } else onResult(false, "Network Error")
        }
    }

    fun saveMessage(userId: String, role: String, content: String) {
        val cleanContent = content.replace("'", "''")
        val sql = "INSERT INTO chat_history (user_id, role, content) VALUES ('$userId', '$role', '$cleanContent');"
        execute(sql) { _, _ -> }
    }

    private fun execute(sql: String, callback: (Boolean, JSONObject?) -> Unit) {
        val body = JSONObject().apply {
            put("requests", JSONArray().put(JSONObject().apply {
                put("type", "execute")
                put("stmt", JSONObject().apply { put("sql", sql) })
            }))
        }.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url("$dbUrl/v2/pipeline")
            .addHeader("Authorization", "Bearer $authToken")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(false, null) }
            override fun onResponse(call: Call, response: Response) {
                val resBody = response.body?.string()
                if (response.isSuccessful && resBody != null) {
                    val json = JSONObject(resBody).getJSONArray("results").getJSONObject(0).getJSONObject("response").getJSONObject("result")
                    callback(true, json)
                } else callback(false, null)
            }
        })
    }
}
