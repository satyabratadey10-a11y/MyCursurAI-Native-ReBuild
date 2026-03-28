package com.turnit.app.ui

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// =====================================================================
// AUTH COLOUR TOKENS (self-contained, no cross-file dependency)
// =====================================================================

private object A {
    val VoidBlack  = Color(0xFF0B0E14)
    val Surface    = Color(0xFF1A1225)
    val Teal       = Color(0xFF008080)
    val TealLight  = Color(0xFF00B8B8)
    val Purple     = Color(0xFF7B4FBF)
    val Blue       = Color(0xFF00D1FF)
    val NeonRed    = Color(0xFFF87171)
    val NeonGreen  = Color(0xFF4ADE80)
    val Glass      = Color(0x1AFFFFFF)
    val GlassBd    = Color(0x33FFFFFF)
    val FieldFill  = Color(0x14FFFFFF)
    val TextPri    = Color(0xFFF0F4FF)
    val TextMuted  = Color(0xFF8A9BB5)
    val ErrRed     = Color(0xFFF87171)
}

// =====================================================================
// AUTH TYPOGRAPHY
// =====================================================================

private val aDisplay = TextStyle(
    fontSize = 28.sp, fontWeight = FontWeight.Bold,
    letterSpacing = 0.06.sp
)
private val aBody = TextStyle(
    fontSize = 14.sp, letterSpacing = 0.01.sp
)
private val aLabel = TextStyle(
    fontSize = 11.sp, letterSpacing = 0.08.sp,
    fontWeight = FontWeight.Medium
)

// =====================================================================
// RGB INTERPOLATION (private, self-contained)
// =====================================================================

private fun aLerp(from: Color, to: Color, t: Float) = Color(
    red   = from.red   + (to.red   - from.red)   * t,
    green = from.green + (to.green - from.green) * t,
    blue  = from.blue  + (to.blue  - from.blue)  * t,
    alpha = 1f
)

private fun aRgb(t: Float): Color {
    val s = listOf(A.NeonRed, A.NeonGreen, A.Blue, A.Purple, A.NeonRed)
    val v = t.coerceIn(0f, 1f) * (s.size - 1)
    val i = v.toInt().coerceIn(0, s.size - 2)
    return aLerp(s[i], s[i + 1], v - i)
}

// =====================================================================
// ROTATING RGB BORDER MODIFIER (auth-local)
// drawWithCache: allocates CornerRadius + Stroke once per size change.
// =====================================================================

@Composable
private fun Modifier.authRgbBorder(
    radius: Float = 14f,
    stroke: Float = 2f,
    dur:    Int   = 2800
): Modifier {
    val inf = rememberInfiniteTransition(label = "auth_rgb")
    val deg by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(dur, easing = LinearEasing),
            RepeatMode.Restart),
        label = "auth_deg"
    )
    val cols = listOf(
        A.NeonRed, A.Teal, A.NeonGreen, A.Blue, A.Purple, A.NeonRed
    )
    return drawWithCache {
        val sw  = stroke
        val cr  = CornerRadius(radius)
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
// NEON FOCUS FIELD
// Border switches from 20%-white to Teal on focus.
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeonField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    modifier:      Modifier     = Modifier,
    kb:            KeyboardType = KeyboardType.Text,
    isPassword:    Boolean      = false
) {
    val focused = remember { mutableStateOf(false) }
    val bdColor = if (focused.value) A.Teal else A.GlassBd
    val shape   = RoundedCornerShape(12.dp)
    TextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = {
            Text(label, style = aBody.copy(
                color = A.TextMuted.copy(alpha = 0.5f)))
        },
        singleLine           = true,
        textStyle            = aBody.copy(color = A.TextPri),
        visualTransformation = if (isPassword)
            PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = kb),
        colors = TextFieldDefaults.colors(
            focusedContainerColor   = A.FieldFill,
            unfocusedContainerColor = A.FieldFill,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor  = Color.Transparent,
            cursorColor             = A.Teal,
            focusedTextColor        = A.TextPri,
            unfocusedTextColor      = A.TextPri
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, bdColor, shape)
    )
}

// =====================================================================
// QUANTUM ENTRY BUTTON
// Teal-to-purple gradient fill + rotating RGB sweep border.
// =====================================================================

@Composable
private fun QuantumButton(
    text:     String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
    enabled:  Boolean  = true
) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick  = onClick,
        enabled  = enabled,
        shape    = shape,
        colors   = ButtonDefaults.buttonColors(
            containerColor         = Color.Transparent,
            contentColor           = A.TextPri,
            disabledContainerColor = A.Glass,
            disabledContentColor   = A.TextMuted
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(A.Teal, A.Purple)), shape
            )
            .authRgbBorder(radius = 14f * 3f, stroke = 2f)
    ) {
        Text(text, style = aBody.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.12.sp,
            color = A.TextPri
        ))
    }
}

// =====================================================================
// AUTH CARD (glassmorphism shell)
// =====================================================================

@Composable
private fun AuthCard(
    modifier: Modifier = Modifier,
    content:  @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        A.Surface.copy(alpha = 0.88f),
                        A.VoidBlack.copy(alpha = 0.96f)
                    )
                ),
                shape
            )
            .border(1.dp, A.GlassBd, shape)
            .padding(horizontal = 28.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

// =====================================================================
// AUTH BACKGROUND (purple top-right + teal bottom-left radial glow)
// No RenderEffect - safe for vivo Y51a (API 30).
// =====================================================================

@Composable
private fun AuthBg(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(A.VoidBlack)
            .background(
                Brush.radialGradient(
                    listOf(A.Purple.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(Float.POSITIVE_INFINITY, 0f),
                    radius = 900f
                )
            )
            .background(
                Brush.radialGradient(
                    listOf(A.Teal.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(0f, Float.POSITIVE_INFINITY),
                    radius = 700f
                )
            )
    )
}

// =====================================================================
// LOGIN SCREEN
// =====================================================================

@Composable
fun LoginScreen(
    onLoginClick:  (username: String, password: String) -> Unit,
    onSignupClick: () -> Unit,
    errorMessage:  String? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        AuthBg()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Gradient logo
            Text(
                "TurnIt",
                style = aDisplay.copy(
                    brush = Brush.linearGradient(
                        listOf(A.Teal, A.Blue, A.Purple)
                    )
                )
            )
            Spacer(Modifier.height(4.dp))
            Text("QUANTUM INTERFACE",
                style = aLabel.copy(color = A.TextMuted))
            Spacer(Modifier.height(36.dp))

            AuthCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("ACCESS NODE",
                        style = aLabel.copy(color = A.Teal))
                    Spacer(Modifier.height(2.dp))
                    NeonField(
                        value = username,
                        onValueChange = { username = it },
                        label = "username"
                    )
                    NeonField(
                        value = password,
                        onValueChange = { password = it },
                        label = "password",
                        kb = KeyboardType.Password,
                        isPassword = true
                    )
                    if (errorMessage != null) {
                        Text(errorMessage,
                            style = aLabel.copy(color = A.ErrRed))
                    }
                    Spacer(Modifier.height(6.dp))
                    QuantumButton(
                        text    = "QUANTUM ENTRY",
                        onClick = {
                            onLoginClick(username.trim(), password)
                        },
                        enabled = username.isNotBlank() &&
                                  password.isNotBlank()
                    )
                    TextButton(onClick = onSignupClick) {
                        Text("New user? Initialize account",
                            style = aBody.copy(color = A.Blue))
                    }
                }
            }
        }
    }
}

// =====================================================================
// SIGNUP SCREEN
// =====================================================================

@Composable
fun SignupScreen(
    onSignupClick: (username: String, email: String, password: String) -> Unit,
    onLoginClick:  () -> Unit,
    errorMessage:  String? = null
) {
    var username by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm  by remember { mutableStateOf("") }

    val match    = password == confirm || confirm.isEmpty()
    val canSubmit = username.isNotBlank() && email.isNotBlank() &&
                    password.isNotBlank() && confirm.isNotBlank() && match

    Box(Modifier.fillMaxSize()) {
        AuthBg()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "TurnIt",
                style = aDisplay.copy(
                    brush = Brush.linearGradient(
                        listOf(A.NeonGreen, A.Blue, A.Purple)
                    )
                )
            )
            Spacer(Modifier.height(4.dp))
            Text("INITIALIZE ACCOUNT",
                style = aLabel.copy(color = A.TextMuted))
            Spacer(Modifier.height(28.dp))

            AuthCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("NEW OPERATOR",
                        style = aLabel.copy(color = A.Teal))
                    Spacer(Modifier.height(2.dp))
                    NeonField(
                        value = username,
                        onValueChange = { username = it },
                        label = "username"
                    )
                    NeonField(
                        value = email,
                        onValueChange = { email = it },
                        label = "email address",
                        kb = KeyboardType.Email
                    )
                    NeonField(
                        value = password,
                        onValueChange = { password = it },
                        label = "password",
                        kb = KeyboardType.Password,
                        isPassword = true
                    )
                    NeonField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = "confirm password",
                        kb = KeyboardType.Password,
                        isPassword = true
                    )
                    if (!match) {
                        Text("Passwords do not match",
                            style = aLabel.copy(color = A.ErrRed))
                    }
                    if (errorMessage != null) {
                        Text(errorMessage,
                            style = aLabel.copy(color = A.ErrRed))
                    }
                    Spacer(Modifier.height(4.dp))
                    QuantumButton(
                        text    = "ACTIVATE OPERATOR",
                        onClick = {
                            onSignupClick(
                                username.trim(),
                                email.trim(),
                                password
                            )
                        },
                        enabled = canSubmit
                    )
                    TextButton(onClick = onLoginClick) {
                        Text("Existing operator? Access node",
                            style = aBody.copy(color = A.Blue))
                    }
                }
            }
        }
    }
}
