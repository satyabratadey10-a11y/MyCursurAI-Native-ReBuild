package com.turnit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.lifecycleScope

// Claude's standard 2026 UI imports
import com.turnit.app.ui.MSG_USER
import com.turnit.app.ui.MSG_AI
import com.turnit.app.ui.TurnItMainScreen
import com.turnit.app.ui.TurnItTheme

/**
 * MainActivity: The Final Bridge
 * Device: vivo Y51a | Project: TurnIt (TuneAi)
 */
class MainActivity : ComponentActivity() {

    private lateinit var reqCtrl: RequestController
    private val messages = mutableStateListOf<Pair<String, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize our RequestController logic
        reqCtrl = RequestController(
            scope = lifecycleScope, 
            geminiKey = BuildConfig.GEMINI_API_KEY, 
            hfKey = BuildConfig.HUGGINGFACE_API_KEY
        )

        setContent {
            TurnItTheme {
                TurnItMainScreen(
                    messages = messages,
                    onSend = { text -> sendMessage(text) },
                    onNewChat = { messages.clear() }
                )
            }
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Add User message to the RIGHT
        messages.add(text to MSG_USER)
        
        // 2. Prepare AI slot on the LEFT
        val aiIndex = messages.size
        messages.add("Thinking..." to MSG_AI)

        // 3. Call our AI Model (Gemini 3 Flash for vivo speed)
        reqCtrl.send(text, "gemini-3-flash-preview", null, { response ->
            messages[aiIndex] = response to MSG_AI
        }, { error ->
            messages[aiIndex] = "Error: $error" to MSG_AI
        })
    }
}
