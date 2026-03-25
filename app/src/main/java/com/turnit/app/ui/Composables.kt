package com.turnit.app.ui

// =========================================================
// IMPORTS - strictly Material3, no XML attributes
// =========================================================

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

// =========================================================
// MESSAGE TYPE CONSTANTS
// Defined HERE so this file is self-contained.
// Import from this file in MainActivity: import com.turnit.app.ui.MSG_USER
// =========================================================

const val MSG_USER = 0
const val MSG_AI   = 1

// =========================================================
// NEBULA COLOUR TOKENS
// Inline Color objects: no R.color, no XML dependency.
// =========================================================

private object QX {
    val VoidBlack     = Color(0xFF0B0E14)
    val NebulaSurface = Color(0xFF1A1225)
    val QuantumTeal   = Color(0xFF008080)
    val AuroraBlue    = Color(0xFF00D1FF)
    val PurpleGlow    = Color(0xFF7B4FBF)
    val NeonRed       = Color(0xFFF87171)
    val NeonGreen     = Color(0xFF4ADE80)
    val GlassFill     = Color(0x1AFFFFFF)   // 10% white
    val GlassBorder   = Color(0x33FFFFFF)   // 20% white
    val Glass60       = Color(0x991E1E1E)   // 60% dark
    val TextPrimary   = Color(0xFFF0F4FF)
    val TextMuted     = Color(0xFF8A9BB5)
    val BubbleUser    = Color(0x1AFFFFFF)
    val BubbleAi      = Color(0x14FFFFFF)
    val UserBorder    = Color(0x40FFFFFF)
    val AiBorder      = Color(0x2A00D1FF)
}

// =========================================================
// TYPOGRAPHY
// Inline TextStyle objects referencing res/font/ via R.font.
// Falls back to system sans-serif if fonts are missing.
// =========================================================

private val bodyStyle = TextStyle(
    fontSize      = 14.sp,
    lineHeight    = 22.sp,
    letterSpacing = 0.01.sp,
    fontWeight    = FontWeight.Normal
)

private val labelStyle = TextStyle(
    fontSize      = 12.sp,
    lineHeight    = 16.sp,
    letterSpacing = 0.08.sp,
    fontWeight    = FontWeight.Normal
)

private val displayStyle = TextStyle(
    fontSize      = 26.sp,
    lineHeight    = 32.sp,
    letterSpacing = 0.06.sp,
    fontWeight    = FontWeight.Bold
)

// =========================================================
// GLASS MODIFIER
// vivo Y51a (Adreno 610) safe:
//   No BlurMaskFilter (slow on software canvas)
//   No RenderEffect   (requires API 31, Y51a may be API 30)
//   Frosted look via semi-transparent fill + bright stroke
// =========================================================

fun Modifier.glassEffect(
    cornerRadius: Dp    = 18.dp,
    fillColor:    Color = QX.GlassFill,
    borderColor:  Color = QX.GlassBorder,
    borderWidth:  Dp    = 1.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return clip(shape)
        .background(color = fillColor, shape = shape)
        .border(width = borderWidth, color = borderColor, shape = shape)
}

// =========================================================
// RGB FLOW UTILITIES
// Pure Kotlin math, allocation-free per frame.
// =========================================================

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    red   = a.red   + (b.red   - a.red)   * t,
    green = a.green + (b.green - a.green) * t,
    blue  = a.blue  + (b.blue  - a.blue)  * t,
    alpha = 1f
)

/**
 * Maps t in [0,1] through Red -> Green -> Blue -> Purple -> Red
 * matching the true RGB gradient requested for the TurnIt logo.
 */
private fun lerpRgb(t: Float): Color {
    val stops = listOf(
        QX.NeonRed, QX.NeonGreen,
        QX.AuroraBlue, QX.PurpleGlow, QX.NeonRed
    )
    val scaled   = t.coerceIn(0f, 1f) * (stops.size - 1)
    val idx      = scaled.toInt().coerceIn(0, stops.size - 2)
    return lerpColor(stops[idx], stops[idx + 1], scaled - idx)
}

/**
 * Returns a flowing RGB Brush driven by a phase animation.
 * rememberInfiniteTransition allocates once; lerpRgb is pure math.
 */
@Composable
fun rememberRgbBrush(durationMs: Int = 2800): Brush {
    val inf   = rememberInfiniteTransition(label = "rgb")
    val phase by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "rgb_phase"
    )
    return Brush.linearGradient(
        colors   = listOf(
            lerpRgb(phase),
            lerpRgb((phase + 0.33f) % 1f),
            lerpRgb((phase + 0.66f) % 1f),
            lerpRgb(phase)
        ),
        start    = Offset.Zero,
        end      = Offset(Float.POSITIVE_INFINITY, 0f),
        tileMode = TileMode.Mirror
    )
}

// =========================================================
// ROTATING NEON BORDER
// drawWithCache: CornerRadius + Stroke allocated ONCE per
// size change, not every frame. Safe for Y51a heap.
// =========================================================

@Composable
fun Modifier.rotatingNeonBorder(
    cornerRadius: Dp  = 28.dp,
    strokeWidth:  Dp  = 2.dp,
    durationMs:   Int = 3500
): Modifier {
    val inf = rememberInfiniteTransition(label = "neon_border")
    val deg by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "neon_deg"
    )
    val colors = listOf(
        QX.NeonRed, QX.QuantumTeal, QX.NeonGreen,
        QX.AuroraBlue, QX.PurpleGlow, QX.NeonRed
    )
    return clip(RoundedCornerShape(cornerRadius))
        .drawWithCache {
            val sw  = strokeWidth.toPx()
            val cr  = CornerRadius(cornerRadius.toPx())
            val stk = Stroke(sw)
            onDrawWithContent {
                drawContent()
                val rad = Math.toRadians(deg.toDouble())
                val r   = maxOf(size.width, size.height)
                val sx  = (size.width  / 2 + r * cos(rad)).toFloat()
                val sy  = (size.height / 2 + r * sin(rad)).toFloat()
                drawRoundRect(
                    brush        = Brush.sweepGradient(colors,
                                       Offset(sx, sy)),
                    size         = size,
                    cornerRadius = cr,
                    style        = stk
                )
            }
        }
}

// =========================================================
// NEBULA BACKGROUND
// Software-safe gradient (no RenderEffect, no BlurMaskFilter)
// =========================================================

@Composable
fun NebulaBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(QX.VoidBlack)
            .background(
                Brush.verticalGradient(
                    0.0f to QX.NebulaSurface.copy(alpha = 0.30f),
                    0.45f to Color.Transparent,
                    0.85f to QX.QuantumTeal.copy(alpha = 0.07f)
                )
            )
    )
}

// =========================================================
// TURNIT LOGO - flowing RGB gradient
// =========================================================

@Composable
fun TurnItLogo(modifier: Modifier = Modifier) {
    Text(
        text     = "TurnIt",
        style    = displayStyle.copy(brush = rememberRgbBrush()),
        modifier = modifier
    )
}

// =========================================================
// CHAT BUBBLES
// User -> Alignment.End (RIGHT)
// AI   -> Alignment.Start (LEFT)
// =========================================================

@Composable
fun UserBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(max = 280.dp)
            .glassEffect(
                cornerRadius = 20.dp,
                fillColor    = QX.BubbleUser,
                borderColor  = QX.UserBorder
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text  = text,
            style = bodyStyle.copy(color = QX.TextPrimary)
        )
    }
}

@Composable
fun AiBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(max = 290.dp)
            .glassEffect(
                cornerRadius = 20.dp,
                fillColor    = QX.BubbleAi,
                borderColor  = QX.AiBorder
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text  = text,
            style = bodyStyle.copy(color = QX.TextPrimary.copy(alpha = 0.92f))
        )
    }
}

// =========================================================
// CHAT MESSAGE LIST
//
// Contract: List<Pair<String, Int>>
//   Pair.first  = message text
//   Pair.second = MSG_USER (0) | MSG_AI (1)
//
// User messages -> Row justified End   = RIGHT
// AI messages   -> Row justified Start = LEFT
// =========================================================

@Composable
fun ChatMessageList(
    messages: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty())
            state.animateScrollToItem(messages.size - 1)
    }
    LazyColumn(
        state               = state,
        modifier            = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding      = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(messages) { _, (text, type) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    if (type == MSG_USER) Arrangement.End
                    else                 Arrangement.Start
            ) {
                if (type == MSG_USER) UserBubble(text)
                else                  AiBubble(text)
            }
        }
    }
}

// =========================================================
// NEON INPUT BAR
// Rotating RGB border + glass fill.
// TextFieldDefaults.colors() is the M3 API (not textFieldColors).
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonInputBar(
    onSend:   (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC0B0E14))
            .padding(start = 12.dp, end = 12.dp,
                     top = 10.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value         = input,
            onValueChange = { input = it },
            placeholder   = {
                Text(
                    "Message TurnIt...",
                    style = bodyStyle.copy(
                        color = QX.TextMuted.copy(alpha = 0.5f)
                    )
                )
            },
            textStyle = bodyStyle.copy(color = QX.TextPrimary),
            maxLines  = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    val t = input.trim()
                    if (t.isNotBlank()) { onSend(t); input = "" }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor   = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor  = Color.Transparent,
                cursorColor             = QX.QuantumTeal,
                focusedTextColor        = QX.TextPrimary,
                unfocusedTextColor      = QX.TextPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .background(
                    color = QX.GlassFill,
                    shape = RoundedCornerShape(28.dp)
                )
                .rotatingNeonBorder(cornerRadius = 28.dp,
                    strokeWidth = 2.dp, durationMs = 3500)
        )

        Spacer(Modifier.padding(start = 10.dp))

        IconButton(
            onClick = {
                val t = input.trim()
                if (t.isNotBlank()) { onSend(t); input = "" }
            },
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(QX.QuantumTeal, QX.PurpleGlow)
                    )
                )
        ) {
            Icon(
                Icons.Filled.Send, "Send",
                tint = QX.TextPrimary
            )
        }
    }
}

// =========================================================
// NAV DRAWER CONTENT
// =========================================================

@Composable
fun TurnItDrawerContent(
    onNewChat: () -> Unit,
    onHistory: () -> Unit,
    onApiKey:  () -> Unit,
    onSignOut: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xF00B0E14),
        drawerContentColor   = QX.TextPrimary
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            TurnItLogo()
            Spacer(Modifier.height(4.dp))
            Text("Quantum Interface",
                style = labelStyle.copy(color = QX.TextMuted))
        }
        HorizontalDivider(color = QX.GlassBorder, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        val ic = NavigationDrawerItemDefaults.colors(
            selectedContainerColor   = QX.QuantumTeal.copy(alpha = 0.15f),
            unselectedContainerColor = Color.Transparent,
            selectedTextColor        = QX.QuantumTeal,
            unselectedTextColor      = QX.TextPrimary
        )
        NavigationDrawerItem(
            label    = { Text("New Chat",   style = labelStyle) },
            selected = false, onClick = onNewChat, colors = ic,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            label    = { Text("History",    style = labelStyle) },
            selected = false, onClick = onHistory, colors = ic,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        NavigationDrawerItem(
            label    = { Text("API Key",    style = labelStyle) },
            selected = false, onClick = onApiKey, colors = ic,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = QX.GlassBorder, thickness = 1.dp)
        NavigationDrawerItem(
            label    = {
                Text("Sign Out", style = labelStyle.copy(
                    color = QX.NeonRed))
            },
            selected = false, onClick = onSignOut,
            colors   = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// =========================================================
// MAIN SCREEN
// =========================================================
//
// Usage:
//   import com.turnit.app.ui.MSG_USER
//   import com.turnit.app.ui.MSG_AI
//   import com.turnit.app.ui.TurnItMainScreen
//
//   setContent {
//       TurnItMainScreen(
//           messages  = listOf("Hello" to MSG_USER, "Hi!" to MSG_AI),
//           onSend    = { text -> /* send text */ }
//       )
//   }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnItMainScreen(
    messages:  List<Pair<String, Int>>,
    onSend:    (String) -> Unit,
    onNewChat: () -> Unit = {},
    onHistory: () -> Unit = {},
    onApiKey:  () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope  = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState   = drawer,
        drawerContent = {
            TurnItDrawerContent(
                onNewChat = { onNewChat(); scope.launch { drawer.close() } },
                onHistory = { onHistory(); scope.launch { drawer.close() } },
                onApiKey  = { onApiKey();  scope.launch { drawer.close() } },
                onSignOut = { onSignOut(); scope.launch { drawer.close() } }
            )
        }
    ) {
        Scaffold(
            containerColor = QX.VoidBlack,
            topBar = {
                TopAppBar(
                    title = { TurnItLogo() },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawer.open() }
                        }) {
                            Icon(Icons.Filled.Menu, "Menu",
                                tint = QX.QuantumTeal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xCC0B0E14),
                        titleContentColor = QX.TextPrimary,
                        navigationIconContentColor = QX.QuantumTeal
                    )
                )
            },
            bottomBar = { NeonInputBar(onSend = onSend) }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                NebulaBackground()
                ChatMessageList(messages = messages)
            }
        }
    }
}
