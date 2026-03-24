package com.turnit.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
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
