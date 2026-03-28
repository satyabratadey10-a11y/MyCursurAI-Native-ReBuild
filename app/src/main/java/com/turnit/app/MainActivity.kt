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
    private lateinit var turso: TursoManager
    private val messages = mutableStateListOf<Pair<String, Int>>()
    
    private var userId by mutableStateOf<String?>(null)
    private var currentScreen by mutableStateOf("login")
    private var activeModel by mutableStateOf(QX_MODELS[0])

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Using the Secrets you added to GitHub
        turso = TursoManager(BuildConfig.TURSO_URL, BuildConfig.TURSO_TOKEN)
        reqCtrl = RequestController(lifecycleScope, BuildConfig.GEMINI_API_KEY, BuildConfig.HUGGINGFACE_API_KEY)

        setContent {
            TurnItTheme {
                when {
                    userId == null && currentScreen == "login" -> {
                        LoginScreen(
                            onLoginClick = { u, p -> 
                                turso.login(u, p) { success, id -> if(success) userId = id }
                            },
                            onSignupClick = { currentScreen = "signup" }
                        )
                    }
                    userId == null && currentScreen == "signup" -> {
                        SignupScreen(
                            onSignupClick = { u, e, p -> 
                                turso.signup(u, e, p) { success, id -> if(success) userId = id }
                            },
                            onLoginClick = { currentScreen = "login" }
                        )
                    }
                    else -> {
                        TurnItMainScreen(
                            messages = messages,
                            selectedModel = activeModel,
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
        if (text.isBlank() || userId == null) return
        messages.add(text to MSG_USER)
        turso.saveMessage(userId!!, "user", text)

        val aiIndex = messages.size
        messages.add("Thinking..." to MSG_AI)

        reqCtrl.send(text, activeModel, null, { response ->
            messages[aiIndex] = response to MSG_AI
            turso.saveMessage(userId!!, "ai", response)
        }, { error ->
            messages[aiIndex] = "Error: $error" to MSG_AI
        })
    }
}
