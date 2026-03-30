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
        execute(sql) { success, _ -> onResult(success, if(success) id else "Signup Failed. Check DB URL/Connection.") }
    }

    fun login(username: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val sql = "SELECT id FROM users WHERE username = '$username' AND password_hash = '$pass';"
        execute(sql) { success, data ->
            try {
                if (success && data != null) {
                    val rows = data.getJSONArray("rows")
                    if (rows.length() > 0) onResult(true, rows.getJSONArray(0).getString(0))
                    else onResult(false, "Invalid Credentials")
                } else onResult(false, "Network Error or Missing Secrets")
            } catch (e: Exception) {
                onResult(false, "Database Parsing Error")
            }
        }
    }

    private fun execute(sql: String, callback: (Boolean, JSONObject?) -> Unit) {
        // Anti-Crash Check 1: Ensure URL is valid before OkHttp tries to parse it
        if (dbUrl.isBlank() || !dbUrl.startsWith("http")) {
            callback(false, null)
            return
        }

        try {
            val body = JSONObject().apply {
                put("requests", JSONArray().put(JSONObject().apply {
                    put("type", "execute")
                    put("stmt", JSONObject().apply { put("sql", sql) })
                }))
            }.toString().toRequestBody(JSON)

            // Anti-Crash Check 2: Safely build the request
            val request = Request.Builder()
                .url("$dbUrl/v2/pipeline")
                .addHeader("Authorization", "Bearer $authToken")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { callback(false, null) }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val resBody = response.body?.string()
                        if (response.isSuccessful && resBody != null) {
                            val json = JSONObject(resBody).getJSONArray("results").getJSONObject(0).getJSONObject("response").getJSONObject("result")
                            callback(true, json)
                        } else callback(false, null)
                    } catch (e: Exception) {
                        // Anti-Crash Check 3: Catch JSON structure changes or errors safely
                        callback(false, null)
                    }
                }
            })
        } catch (e: Exception) {
            callback(false, null)
        }
    }
}
