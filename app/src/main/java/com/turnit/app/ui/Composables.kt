package com.turnit.app.ui

// ModelOption and QX_MODELS imported from the models package
import com.turnit.app.models.ModelOption
import com.turnit.app.models.QX_MODELS

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

// =====================================================================
// MESSAGE TYPE CONSTANTS
// import com.turnit.app.ui.MSG_USER / MSG_AI
// =====================================================================

const val MSG_USER = 0
const val MSG_AI   = 1

// =====================================================================
// QX COLOUR PALETTE
// =====================================================================

private object QX {
    val VoidBlack  = Color(0xFF0B0E14)
    val Surface    = Color(0xFF1A1225)
    val Teal       = Color(0xFF008080)
    val TealLight  = Color(0xFF00B8B8)
    val Purple     = Color(0xFF7B4FBF)
    val Blue       = Color(0xFF00D1FF)
    val NeonRed    = Color(0xFFF87171)
    val NeonGreen  = Color(0xFF4ADE80)
    val GlassFill  = Color(0x1AFFFFFF)
    val GlassBd    = Color(0x33FFFFFF)
    // User bubble: teal-tinted glass
    val BubbleUser = Color(0x1A008080)
    val BdUser     = Color(0x5500B8B8)
    // AI bubble: purple-tinted glass
    val BubbleAi   = Color(0x1A7B4FBF)
    val BdAi       = Color(0x557B4FBF)
    val TextPri    = Color(0xFFF0F4FF)
    val TextMuted  = Color(0xFF8A9BB5)
}

// =====================================================================
// TYPOGRAPHY
// =====================================================================

private val bodyStyle = TextStyle(
    fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.01.sp
)
private val labelStyle = TextStyle(
    fontSize = 11.sp, letterSpacing = 0.08.sp,
    fontWeight = FontWeight.Medium
)
private val displayStyle = TextStyle(
    fontSize = 26.sp, fontWeight = FontWeight.Bold,
    letterSpacing = 0.06.sp
)

// =====================================================================
// RGB INTERPOLATION
// =====================================================================

private fun lerpCol(a: Color, b: Color, t: Float) = Color(
    red   = a.red   + (b.red   - a.red)   * t,
    green = a.green + (b.green - a.green) * t,
    blue  = a.blue  + (b.blue  - a.blue)  * t,
    alpha = 1f
)

private fun lerpRgb(t: Float): Color {
    val s = listOf(QX.NeonRed, QX.NeonGreen, QX.Blue, QX.Purple, QX.NeonRed)
    val v = t.coerceIn(0f, 1f) * (s.size - 1)
    val i = v.toInt().coerceIn(0, s.size - 2)
    return lerpCol(s[i], s[i + 1], v - i)
}

@Composable
fun rememberRgbBrush(durationMs: Int = 2800): Brush {
    val inf   = rememberInfiniteTransition(label = "rgb")
    val phase by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(durationMs, easing = LinearEasing),
            RepeatMode.Restart),
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

// =====================================================================
// ROTATING NEON BORDER MODIFIER
// drawWithCache: Stroke + CornerRadius allocated once per size.
// Safe on vivo Y51a (Adreno 610, API 30).
// =====================================================================

@Composable
fun Modifier.rotatingNeonBorder(
    cornerRadius: Dp  = 28.dp,
    strokeWidth:  Dp  = 2.dp,
    durationMs:   Int = 3500
): Modifier {
    val inf = rememberInfiniteTransition(label = "neon_bd")
    val deg by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(durationMs, easing = LinearEasing),
            RepeatMode.Restart),
        label = "neon_deg"
    )
    val cols = listOf(
        QX.NeonRed, QX.Teal, QX.NeonGreen,
        QX.Blue, QX.Purple, QX.NeonRed
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
                    brush        = Brush.sweepGradient(cols, Offset(sx, sy)),
                    size         = size,
                    cornerRadius = cr,
                    style        = stk
                )
            }
        }
}

// =====================================================================
// GLASS MODIFIER
// =====================================================================

fun Modifier.glassEffect(
    cornerRadius: Dp    = 12.dp,
    fill:         Color = QX.GlassFill,
    border:       Color = QX.GlassBd,
    borderWidth:  Dp    = 1.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return clip(shape)
        .background(color = fill, shape = shape)
        .border(width = borderWidth, color = border, shape = shape)
}

// =====================================================================
// NEBULA BACKGROUND
// =====================================================================

@Composable
fun NebulaBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(QX.VoidBlack)
            .background(
                Brush.verticalGradient(
                    0.0f  to QX.Surface.copy(alpha = 0.28f),
                    0.45f to Color.Transparent,
                    0.85f to QX.Teal.copy(alpha = 0.06f)
                )
            )
    )
}

// =====================================================================
// TURNIT LOGO
// =====================================================================

@Composable
fun TurnItLogo(modifier: Modifier = Modifier) {
    Text(
        "TurnIt",
        style    = displayStyle.copy(brush = rememberRgbBrush()),
        modifier = modifier
    )
}

// =====================================================================
// CHAT BUBBLES
// 12dp corner radius, 1dp translucent border.
// User: teal-tinted glass  (Alignment.End  = RIGHT)
// AI:   purple-tinted glass (Alignment.Start = LEFT)
// =====================================================================

@Composable
fun UserBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(max = 280.dp)
            .glassEffect(
                cornerRadius = 12.dp,
                fill         = QX.BubbleUser,
                border       = QX.BdUser
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, style = bodyStyle.copy(color = QX.TextPri))
    }
}

@Composable
fun AiBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .widthIn(max = 290.dp)
            .glassEffect(
                cornerRadius = 12.dp,
                fill         = QX.BubbleAi,
                border       = QX.BdAi
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, style = bodyStyle.copy(
            color = QX.TextPri.copy(alpha = 0.92f)))
    }
}

// =====================================================================
// CHAT MESSAGE LIST
// Contract: List<Pair<String, Int>>
//   .first  = text
//   .second = MSG_USER (0) | MSG_AI (1)
// =====================================================================

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
                    else Arrangement.Start
            ) {
                if (type == MSG_USER) UserBubble(text)
                else AiBubble(text)
            }
        }
    }
}

// =====================================================================
// FLOATING MODEL SELECTOR
//
// A 48dp glassmorphic circle with an RGB rotating border.
// Positioned at absolute bottom-right, 8dp above the input bar.
//
// When tapped, a scrollable "Nebula" card expands UPWARD listing
// all QX_MODELS. Tapping a model collapses the list and updates
// the icon label.
//
// Composable placement in the parent:
//   Box(Modifier.fillMaxSize()) {
//     ChatMessageList(messages)
//     FloatingModelSelector(
//         selected = selectedModel,
//         onSelect = { selectedModel = it },
//         modifier = Modifier
//             .align(Alignment.BottomEnd)
//             .padding(end = 12.dp, bottom = 8.dp)
//     )
//   }
// =====================================================================

@Composable
fun FloatingModelSelector(
    selected: ModelOption,
    onSelect: (ModelOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ---- Upward-expanding model list ----
        if (expanded) {
            Column(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 220.dp)
                    .heightIn(max = 220.dp)
                    // Nebula card
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                QX.Surface.copy(alpha = 0.95f),
                                QX.VoidBlack.copy(alpha = 0.98f)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp, QX.GlassBd, RoundedCornerShape(16.dp)
                    )
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    "SELECT ENGINE",
                    style = labelStyle.copy(
                        color = QX.Teal,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(
                        start = 16.dp, end = 16.dp,
                        top = 12.dp, bottom = 8.dp
                    )
                )
                HorizontalDivider(
                    color = QX.GlassBd, thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                QX_MODELS.forEach { model ->
                    val isActive = model.id == selected.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(model)
                                expanded = false
                            }
                            .background(
                                if (isActive)
                                    QX.Teal.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .padding(
                                horizontal = 16.dp,
                                vertical   = 10.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                model.displayName,
                                style = bodyStyle.copy(
                                    color = if (isActive)
                                        QX.TealLight else QX.TextPri,
                                    fontWeight = if (isActive)
                                        FontWeight.SemiBold
                                    else FontWeight.Normal
                                )
                            )
                            if (isActive) {
                                Text(
                                    "active",
                                    style = labelStyle.copy(
                                        color    = QX.Teal,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                        // Short label badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive)
                                        QX.Teal.copy(alpha = 0.25f)
                                    else QX.GlassFill
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                model.shortLabel,
                                style = labelStyle.copy(
                                    fontSize = 8.sp,
                                    color = if (isActive)
                                        QX.TealLight else QX.TextMuted
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // ---- 48dp glass circle icon ----
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                // Glass fill with subtle purple-teal radial
                .background(
                    Brush.radialGradient(
                        listOf(
                            QX.Teal.copy(alpha = 0.30f),
                            QX.Purple.copy(alpha = 0.18f),
                            QX.GlassFill
                        )
                    )
                )
                // RGB rotating border
                .rotatingNeonBorder(
                    cornerRadius = 24.dp,
                    strokeWidth  = 2.dp,
                    durationMs   = 2500
                )
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center
        ) {
            Text(
                selected.shortLabel,
                style = labelStyle.copy(
                    fontSize = 10.sp,
                    color    = if (expanded) QX.TealLight else QX.TextPri
                )
            )
        }
    }
}

// =====================================================================
// NEON INPUT BAR
// =====================================================================

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
                     top = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value         = input,
            onValueChange = { input = it },
            placeholder   = {
                Text("Message TurnIt...",
                    style = bodyStyle.copy(
                        color = QX.TextMuted.copy(alpha = 0.5f)
                    ))
            },
            textStyle = bodyStyle.copy(color = QX.TextPri),
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
                cursorColor             = QX.Teal,
                focusedTextColor        = QX.TextPri,
                unfocusedTextColor      = QX.TextPri
            ),
            modifier = Modifier
                .weight(1f)
                .background(QX.GlassFill, RoundedCornerShape(28.dp))
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
                    Brush.linearGradient(listOf(QX.Teal, QX.Purple))
                )
        ) {
            Icon(Icons.Filled.Send, "Send", tint = QX.TextPri)
        }
    }
}

// =====================================================================
// NAV DRAWER CONTENT
// =====================================================================

@Composable
fun TurnItDrawerContent(
    onNewChat: () -> Unit,
    onHistory: () -> Unit,
    onApiKey:  () -> Unit,
    onSignOut: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xF00B0E14),
        drawerContentColor   = QX.TextPri
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            TurnItLogo()
            Spacer(Modifier.height(4.dp))
            Text("Quantum Interface",
                style = labelStyle.copy(color = QX.TextMuted))
        }
        HorizontalDivider(color = QX.GlassBd, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        val ic = NavigationDrawerItemDefaults.colors(
            selectedContainerColor   = QX.Teal.copy(alpha = 0.15f),
            unselectedContainerColor = Color.Transparent,
            selectedTextColor        = QX.Teal,
            unselectedTextColor      = QX.TextPri
        )
        listOf("New Chat" to onNewChat, "History" to onHistory,
               "API Key"  to onApiKey).forEach { (lbl, act) ->
            NavigationDrawerItem(
                label    = { Text(lbl, style = labelStyle) },
                selected = false, onClick = act, colors = ic,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = QX.GlassBd, thickness = 1.dp)
        NavigationDrawerItem(
            label    = { Text("Sign Out", style = labelStyle.copy(
                color = QX.NeonRed)) },
            selected = false, onClick = onSignOut,
            colors   = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// =====================================================================
// TURNIT MAIN SCREEN
//
// The FloatingModelSelector is placed inside the content Box at
// Alignment.BottomEnd with a bottom offset of 8dp so it floats
// exactly 8dp above the NeonInputBar bottom bar.
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnItMainScreen(
    messages:     List<Pair<String, Int>>,
    onSend:       (String) -> Unit,
    selectedModel: ModelOption      = QX_MODELS[0],
    onModelSelect: (ModelOption) -> Unit = {},
    onNewChat:    () -> Unit = {},
    onHistory:    () -> Unit = {},
    onApiKey:     () -> Unit = {},
    onSignOut:    () -> Unit = {}
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
                                tint = QX.Teal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor      = Color(0xCC0B0E14),
                        titleContentColor   = QX.TextPri,
                        navigationIconColor = QX.Teal
                    )
                )
            },
            bottomBar = { NeonInputBar(onSend = onSend) }
        ) { pad ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {
                NebulaBackground()
                ChatMessageList(messages = messages)

                // Floating model selector:
                // pinned bottom-right, offset 8dp from bottom edge
                // (sits just above the NeonInputBar bottom bar).
                FloatingModelSelector(
                    selected = selectedModel,
                    onSelect = onModelSelect,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 8.dp)
                )
            }
        }
    }
}
