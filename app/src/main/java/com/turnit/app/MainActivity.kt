package com.turnit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.turnit.app.ui.*
import com.turnit.app.models.ModelOption

class MainActivity : ComponentActivity() {
    private lateinit var reqCtrl: RequestController
    private val messages = mutableStateListOf<Pair<String, Int>>()
    
    // Auth State (Problem 2)
    private var isLoggedIn by mutableStateOf(false)

    // Global Model State (Problem 1 & 5 Fix)
    private var activeModel by mutableStateOf(QX_MODELS[0])

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reqCtrl = RequestController(lifecycleScope, BuildConfig.GEMINI_API_KEY, BuildConfig.HUGGINGFACE_API_KEY)

        setContent {
            TurnItTheme {
                if (!isLoggedIn) {
                    LoginScreen(
                        onLoginClick = { _, _ -> isLoggedIn = true },
                        onSignupClick = { /* Navigate to Signup UI */ }
                    )
                } else {
                    TurnItMainScreen(
                        messages = messages,
                        initialModel = activeModel,
                        onModelChange = { activeModel = it },
                        onSend = { text -> sendMessage(text) },
                        onNewChat = { messages.clear() } // Problem 4 Fix
                    )
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(text to MSG_USER)
        val aiIndex = messages.size
        messages.add("Thinking..." to MSG_AI)

        // Logic Connection (Problem 5)
        reqCtrl.send(text, activeModel, null, { response ->
            messages[aiIndex] = response to MSG_AI
        }, { error ->
            messages[aiIndex] = "Error: $error" to MSG_AI
        })
    }
}
