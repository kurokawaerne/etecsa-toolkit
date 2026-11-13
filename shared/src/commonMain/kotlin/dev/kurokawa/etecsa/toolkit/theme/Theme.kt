package dev.kurokawa.etecsa.toolkit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kurokawa.etecsa.toolkit.resources.Res
import dev.kurokawa.etecsa.toolkit.resources.google_sans_flex
import org.jetbrains.compose.resources.Font

// ETECSA brand: deep telecom blue + signal teal.
internal val LightScheme = lightColorScheme(
    primary = Color(0xFF0057A8), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF), onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFF00696D), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF6FF7FB), onSecondaryContainer = Color(0xFF002021),
    tertiary = Color(0xFF5A5BD6),
    surface = Color(0xFFF7F9FC), onSurface = Color(0xFF191C1E),
)
internal val DarkScheme = darkColorScheme(
    primary = Color(0xFFA4C8FF), onPrimary = Color(0xFF00315E),
    primaryContainer = Color(0xFF004784), onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFF4CDADD), onSecondary = Color(0xFF003738),
    secondaryContainer = Color(0xFF004F52), onSecondaryContainer = Color(0xFF6FF7FB),
    tertiary = Color(0xFFC0C1FF),
    surface = Color(0xFF101418), onSurface = Color(0xFFE0E3E8),
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
private fun sansFlexFamily(width: Float, roundness: Float): FontFamily {
    fun settings(weight: Int) = FontVariation.Settings(
        FontVariation.weight(weight), FontVariation.width(width),
        FontVariation.Setting("ROND", roundness),
    )
    val normal = Font(Res.font.google_sans_flex, FontWeight.Normal, variationSettings = settings(400))
    val medium = Font(Res.font.google_sans_flex, FontWeight.Medium, variationSettings = settings(500))
    val semiBold = Font(Res.font.google_sans_flex, FontWeight.SemiBold, variationSettings = settings(620))
    val bold = Font(Res.font.google_sans_flex, FontWeight.Bold, variationSettings = settings(720))
    return remember(normal, medium, semiBold, bold) { FontFamily(normal, medium, semiBold, bold) }
}

@Composable
private fun expressiveTypography(): Typography {
    val display = sansFlexFamily(118f, 100f)
    val text = sansFlexFamily(100f, 35f)
    val base = Typography()
    return remember(display, text) {
        Typography(
            displayLarge = base.displayLarge.copy(fontFamily = display, fontWeight = FontWeight.Bold),
            displayMedium = base.displayMedium.copy(fontFamily = display, fontWeight = FontWeight.Bold),
            displaySmall = base.displaySmall.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
            headlineLarge = base.headlineLarge.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
            headlineMedium = base.headlineMedium.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
            headlineSmall = base.headlineSmall.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
            titleLarge = base.titleLarge.copy(fontFamily = display, fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontFamily = text, fontWeight = FontWeight.Medium),
            titleSmall = base.titleSmall.copy(fontFamily = text, fontWeight = FontWeight.Medium),
            bodyLarge = base.bodyLarge.copy(fontFamily = text),
            bodyMedium = base.bodyMedium.copy(fontFamily = text),
            bodySmall = base.bodySmall.copy(fontFamily = text),
            labelLarge = base.labelLarge.copy(fontFamily = text, fontWeight = FontWeight.Medium),
            labelMedium = base.labelMedium.copy(fontFamily = text, fontWeight = FontWeight.Medium),
            labelSmall = base.labelSmall.copy(fontFamily = text, fontWeight = FontWeight.Medium),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EtecsaToolkitTheme(darkMode: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val motion = remember { MotionScheme.expressive() }
    MaterialExpressiveTheme(
        colorScheme = if (darkMode) DarkScheme else LightScheme,
        motionScheme = motion, shapes = ExpressiveShapes,
        typography = expressiveTypography(), content = content,
    )
}
