package com.turnit.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// Import our unified data model
import com.turnit.app.models.ModelOption

const val MSG_USER = 0
const val MSG_AI   = 1

private object QX {
    val VoidBlack      = Color(0xFF0B0E14)
    val QuantumTeal    = Color(0xFF008080)
    val PurpleGlow     = Color(0xFF7B4FBF)
    val NeonRed        = Color(0xFFF87171)
    val GlassFill      = Color(0x1AFFFFFF)
    val GlassBorder    = Color(0x33FFFFFF)
    val TextPrimary    = Color(0xFFF0F4FF)
    val TextMuted      = Color(0xFF8A9BB5)
}

@Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnItMainScreen(
    messages: List<Pair<String, Int>>,
    initialModel: ModelOption,
    onModelChange: (ModelOption) -> Unit,
    onSend: (String) -> Unit,
    onNewChat: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = QX.VoidBlack,
                drawerContentColor = QX.TextPrimary
            ) {
                Text("TurnIt History", modifier = Modifier.padding(16.dp), style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                NavigationDrawerItem(
                    label = { Text("New Chat") },
                    selected = false,
                    onClick = { onNewChat(); scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = QX.VoidBlack,
            topBar = {
                TopAppBar(
                    title = { Text("TurnIt", color = QX.QuantumTeal, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menu", tint = QX.QuantumTeal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = QX.VoidBlack)
                )
            },
            bottomBar = {
                NeonInputBar(onSend = onSend)
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                ChatList(messages)
            }
        }
    }
}

@Composable
fun ChatList(messages: List<Pair<String, Int>>) {
    val state = rememberLazyListState()
    LaunchedEffect(messages.size) { if(messages.isNotEmpty()) state.animateScrollToItem(messages.size - 1) }
    LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        itemsIndexed(messages) { _, msg ->
            val align = if(msg.second == MSG_USER) Alignment.CenterEnd else Alignment.CenterStart
            val color = if(msg.second == MSG_USER) QX.QuantumTeal.copy(alpha = 0.2f) else QX.GlassFill
            Box(Modifier.fillMaxWidth(), contentAlignment = align) {
                Text(
                    text = msg.first,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .background(color, RoundedCornerShape(12.dp))
                        .border(1.dp, QX.GlassBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    color = QX.TextPrimary
                )
            }
        }
    }
}

@Composable
fun NeonInputBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f).background(QX.GlassFill, CircleShape).border(1.dp, QX.QuantumTeal, CircleShape),
            placeholder = { Text("Message...", color = QX.TextMuted) },
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend(text); text = "" })
        )
        IconButton(onClick = { onSend(text); text = "" }) {
            Icon(Icons.Default.Send, "Send", tint = QX.QuantumTeal)
        }
    }
}
