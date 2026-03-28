package com.turnit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.* // This provides getValue and setValue
import androidx.compose.ui.Modifier
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
            "", 
            ""
        )

        setContent {
            TurnItTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isLoggedIn) {
                        // FIX: Added required imports for Button and Text
                        Button(
                            onClick = { isLoggedIn = true }, 
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("ENTER TURNIT")
                        }
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
        
        // Pass the CURRENT active model to the AI controller
        reqCtrl.send(text, activeModel, null, { response ->
            messages.add(response to MSG_AI)
        }, { error ->
            // Handle error
        })
    }
}
