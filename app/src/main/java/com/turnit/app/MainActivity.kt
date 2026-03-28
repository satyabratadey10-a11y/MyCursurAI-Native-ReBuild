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
    
    // Logic for Login and Model Switching
    private var isLoggedIn by mutableStateOf(false)
    private var activeModel by mutableStateOf(QX_MODELS[0])

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        reqCtrl = RequestController(
            lifecycleScope, 
            BuildConfig.GEMINI_API_KEY, 
            BuildConfig.HUGGINGFACE_API_KEY
        )

        setContent {
            // This calls the theme defined in Theme.kt
            TurnItTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (!isLoggedIn) {
                        LoginScreen(
                            onLoginClick = { _, _ -> isLoggedIn = true },
                            onSignupClick = { /* Link to signup if needed */ }
                        )
                    } else {
                        TurnItMainScreen(
                            messages = messages,
                            initialModel = activeModel,
                            onModelChange = { activeModel = it },
                            onSend = { text -> sendMessage(text) },
                            onNewChat = { messages.clear() }
                        )
                    }
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        messages.add(text to MSG_USER)
        val aiIndex = messages.size
        messages.add("Thinking..." to MSG_AI)

        reqCtrl.send(text, activeModel, null, { response ->
            messages[aiIndex] = response to MSG_AI
        }, { error ->
            messages[aiIndex] = "Error: $error" to MSG_AI
        })
    }
}
