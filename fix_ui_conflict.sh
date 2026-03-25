#!/bin/bash
# 1. Update Theme.kt
cat << 'KOTLIN' > app/src/main/java/com/turnit/app/ui/Theme.kt
package com.turnit.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.turnit.app.R

object NebulaColors {
    val VoidBlack = Color(0xFF0B0E14)
    val NebulaSurface = Color(0xFF1A1225)
    val QuantumTeal = Color(0xFF008080)
    val GlassBorder = Color(0x33FFFFFF)
}

private val TurnItColorScheme = darkColorScheme(
    primary = NebulaColors.QuantumTeal,
    background = NebulaColors.VoidBlack,
    surface = NebulaColors.NebulaSurface,
    outline = NebulaColors.GlassBorder
)

val EquinoxFamily = FontFamily(Font(R.font.equinox, FontWeight.Normal))
val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_bold, FontWeight.Bold)
)

val TurnItTypography = Typography(
    displayMedium = TextStyle(fontFamily = EquinoxFamily, fontSize = 28.sp, letterSpacing = 0.06.sp),
    bodyMedium = TextStyle(fontFamily = SpaceGroteskFamily, fontSize = 14.sp)
)

@Composable
fun TurnItTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TurnItColorScheme, typography = TurnItTypography, content = content)
}
KOTLIN

# 2. Update Composables.kt (Constants stay here)
cat << 'KOTLIN' > app/src/main/java/com/turnit/app/ui/Composables.kt
package com.turnit.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch

// DEFINED ONLY HERE TO PREVENT COMPILATION ERRORS
const val MSG_USER = 0
const val MSG_AI = 1

@Composable
fun rememberRgbBrush(): Brush {
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing))
    )
    return Brush.linearGradient(
        colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Red),
        start = Offset(phase * 1000f, 0f),
        end = Offset(phase * 1000f + 500f, 500f),
        tileMode = TileMode.Mirror
    )
}

@Composable
fun TurnItLogo(modifier: Modifier = Modifier) {
    Text(
        text = "TurnIt",
        style = MaterialTheme.typography.displayMedium.copy(brush = rememberRgbBrush()),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnItMainScreen(
    messages: List<Pair<String, Int>>,
    onSend: (String) -> Unit,
    onNewChat: () -> Unit,
    onApiKey: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF0B0E14)) {
                Spacer(Modifier.height(12.dp))
                TurnItLogo(Modifier.padding(16.dp))
                NavigationDrawerItem(label = { Text("New Chat") }, selected = false, onClick = { onNewChat(); scope.launch { drawerState.close() } })
                NavigationDrawerItem(label = { Text("API Key Settings") }, selected = false, onClick = { onApiKey(); scope.launch { drawerState.close() } })
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { TurnItLogo() },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            },
            bottomBar = {
                NeonInputBar(onSend)
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(Color.Black)) {
                ChatList(messages)
            }
        }
    }
}

@Composable
fun ChatList(messages: List<Pair<String, Int>>) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if(messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
    
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(8.dp)) {
        items(messages) { (text, type) ->
            Box(Modifier.fillMaxWidth(), contentAlignment = if (type == MSG_USER) Alignment.CenterEnd else Alignment.CenterStart) {
                Text(
                    text = text,
                    color = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .background(if (type == MSG_USER) Color(0x334285F4) else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonInputBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f).border(2.dp, rememberRgbBrush(), RoundedCornerShape(24.dp)),
            colors = TextFieldDefaults.textFieldColors(containerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
        )
        IconButton(onClick = { if (text.isNotBlank()) { onSend(text); text = "" } }) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Cyan)
        }
    }
}
KOTLIN
