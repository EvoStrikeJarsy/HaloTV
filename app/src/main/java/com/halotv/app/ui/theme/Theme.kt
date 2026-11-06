package com.halotv.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import com.halotv.app.R

object HaloColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF1C1C1E)
    val SurfaceFocused = Color(0xFF2C2C2E)
    val Glass = Color(0xD91C1C1E)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF8E8E93)
    val Accent = Color(0xFF64A0FF)
    val Live = Color(0xFFFF375F)
    val Gold = Color(0xFFFFD60A)
    val CardGradient = Brush.verticalGradient(listOf(Color(0xFF2C2C30), Color(0xFF151517)))
}

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val base = Typography()
private val HaloTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontFamily = Inter),
    bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
    bodySmall = base.bodySmall.copy(fontFamily = Inter),
    labelLarge = base.labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
    labelSmall = base.labelSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Medium),
)

@Composable
fun HaloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            background = HaloColors.Background,
            onBackground = HaloColors.TextPrimary,
            surface = HaloColors.Surface,
            onSurface = HaloColors.TextPrimary,
            surfaceVariant = HaloColors.SurfaceFocused,
            onSurfaceVariant = HaloColors.TextSecondary,
        ),
        typography = HaloTypography,
        content = content,
    )
}
