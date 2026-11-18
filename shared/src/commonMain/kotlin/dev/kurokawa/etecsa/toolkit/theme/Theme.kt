package dev.kurokawa.etecsa.toolkit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kurokawa.etecsa.toolkit.resources.Res
import dev.kurokawa.etecsa.toolkit.resources.gsf_display_bold
import dev.kurokawa.etecsa.toolkit.resources.gsf_display_extrabold
import dev.kurokawa.etecsa.toolkit.resources.gsf_display_medium
import dev.kurokawa.etecsa.toolkit.resources.gsf_display_semibold
import dev.kurokawa.etecsa.toolkit.resources.gsf_text_bold
import dev.kurokawa.etecsa.toolkit.resources.gsf_text_medium
import dev.kurokawa.etecsa.toolkit.resources.gsf_text_regular
import dev.kurokawa.etecsa.toolkit.resources.gsf_text_semibold
import org.jetbrains.compose.resources.Font

// ETECSA brand: deep telecom blue + signal teal. Gradient A (Ernest's
// pick, Google-2025 diagonal: blue -> violet -> pink) is the identity
// carried by the mark, the consult hero card and the splash.
val BrandBlue = Color(0xFF0099E8)
val BrandViolet = Color(0xFF7B3FE4)
val BrandPink = Color(0xFFD63FA0)
val BrandGradient: Brush = Brush.linearGradient(
    listOf(BrandBlue, BrandViolet, BrandPink),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(400f, 400f),
)
internal val LightScheme = lightColorScheme(
    primary = Color(0xFF0057A8), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF), onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFF00696D), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF6FF7FB), onSecondaryContainer = Color(0xFF002021),
    tertiary = BrandViolet,
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

/**
 * Google Sans Flex as STATIC instances — the same 9 files LSEx uses
 * (v0.10.0 lesson): the VARIABLE font renders only at its default
 * instance on Android, so every weight and the ROND axis were ignored
 * there — headings looked wrong on Ernest's phone while desktop (Skia
 * honours variationSettings) looked right. Static instances render the
 * same on both. Display = wdth 118 / ROND 100; text = wdth 100 / ROND 35.
 */
@Composable
private fun textFamily(): FontFamily {
    val regular = Font(Res.font.gsf_text_regular, FontWeight.Normal)
    val medium = Font(Res.font.gsf_text_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.gsf_text_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.gsf_text_bold, FontWeight.Bold)
    return remember(regular, medium, semiBold, bold) {
        FontFamily(regular, medium, semiBold, bold)
    }
}

@Composable
private fun displayFamily(): FontFamily {
    val medium = Font(Res.font.gsf_display_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.gsf_display_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.gsf_display_bold, FontWeight.Bold)
    val extraBold = Font(Res.font.gsf_display_extrabold, FontWeight.ExtraBold)
    return remember(medium, semiBold, bold, extraBold) {
        FontFamily(medium, semiBold, bold, extraBold)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun expressiveTypography(): Typography {
    val display = displayFamily()
    val text = textFamily()
    val base = Typography()
    return remember(display, text) {
        // LSEx/UDYAT rule: EVERY style — base AND emphasized — is set in
        // a GSF cut. Before, only the 15 base styles were mapped, so every
        // *Emphasized style (group headers, nav rows, emphasized buttons)
        // silently fell back to the platform font.
        fun styled(
            style: androidx.compose.ui.text.TextStyle,
            family: FontFamily,
            weight: FontWeight,
        ) = style.copy(fontFamily = family, fontWeight = weight)
        Typography(
            displayLarge = styled(base.displayLarge, display, FontWeight.Bold),
            displayMedium = styled(base.displayMedium, display, FontWeight.Bold),
            displaySmall = styled(base.displaySmall, display, FontWeight.SemiBold),
            displayLargeEmphasized = styled(base.displayLargeEmphasized, display, FontWeight.ExtraBold),
            displayMediumEmphasized = styled(base.displayMediumEmphasized, display, FontWeight.ExtraBold),
            displaySmallEmphasized = styled(base.displaySmallEmphasized, display, FontWeight.Bold),
            headlineLarge = styled(base.headlineLarge, display, FontWeight.SemiBold),
            headlineMedium = styled(base.headlineMedium, display, FontWeight.SemiBold),
            headlineSmall = styled(base.headlineSmall, display, FontWeight.SemiBold),
            headlineLargeEmphasized = styled(base.headlineLargeEmphasized, display, FontWeight.Bold),
            headlineMediumEmphasized = styled(base.headlineMediumEmphasized, display, FontWeight.Bold),
            headlineSmallEmphasized = styled(base.headlineSmallEmphasized, display, FontWeight.Bold),
            titleLarge = styled(base.titleLarge, display, FontWeight.SemiBold),
            titleMedium = styled(base.titleMedium, text, FontWeight.Medium),
            titleSmall = styled(base.titleSmall, text, FontWeight.Medium),
            titleLargeEmphasized = styled(base.titleLargeEmphasized, display, FontWeight.Bold),
            titleMediumEmphasized = styled(base.titleMediumEmphasized, text, FontWeight.SemiBold),
            titleSmallEmphasized = styled(base.titleSmallEmphasized, text, FontWeight.SemiBold),
            bodyLarge = styled(base.bodyLarge, text, FontWeight.Normal),
            bodyMedium = styled(base.bodyMedium, text, FontWeight.Normal),
            bodySmall = styled(base.bodySmall, text, FontWeight.Normal),
            bodyLargeEmphasized = styled(base.bodyLargeEmphasized, text, FontWeight.Medium),
            bodyMediumEmphasized = styled(base.bodyMediumEmphasized, text, FontWeight.Medium),
            bodySmallEmphasized = styled(base.bodySmallEmphasized, text, FontWeight.Medium),
            labelLarge = styled(base.labelLarge, text, FontWeight.Medium),
            labelMedium = styled(base.labelMedium, text, FontWeight.Medium),
            labelSmall = styled(base.labelSmall, text, FontWeight.Medium),
            labelLargeEmphasized = styled(base.labelLargeEmphasized, text, FontWeight.SemiBold),
            labelMediumEmphasized = styled(base.labelMediumEmphasized, text, FontWeight.SemiBold),
            labelSmallEmphasized = styled(base.labelSmallEmphasized, text, FontWeight.SemiBold),
        )
    }
}

// Wallpaper-derived scheme on Android (API 31+); null where the
// platform has none (desktop harness). Ernest's verdict (2026-11-17):
// "Where are the dynamic things" — the theme used only the fixed
// LightScheme/DarkScheme, so on his phone the app never picked up his
// wallpaper colors. Dynamic color is now the scheme when available.
@Composable
expect fun platformDynamicColorScheme(dark: Boolean): ColorScheme?

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EtecsaToolkitTheme(darkMode: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val motion = remember { MotionScheme.expressive() }
    val dynamic = platformDynamicColorScheme(darkMode)
    val scheme = dynamic ?: if (darkMode) DarkScheme else LightScheme
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = motion, shapes = ExpressiveShapes,
        typography = expressiveTypography(), content = content,
    )
}
