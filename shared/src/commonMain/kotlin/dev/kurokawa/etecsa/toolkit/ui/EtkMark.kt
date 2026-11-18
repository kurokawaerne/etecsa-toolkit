package dev.kurokawa.etecsa.toolkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.kurokawa.etecsa.toolkit.theme.BrandGradient

/** Ernest's chosen mark: the ETECSA "e" disc, converted verbatim from
 *  his SVG (etk-icon-from-ernest.svg, 38x38 viewBox, path translated
 *  +3.167/+3.167 by the SVG's own transform). The launcher icon uses the
 *  same path baked into the 108dp adaptive viewport (18..90). */
val EtkMarkVector: ImageVector = ImageVector.Builder(
    name = "etk_e_disc", defaultWidth = 38.dp, defaultHeight = 38.dp,
    viewportWidth = 38f, viewportHeight = 38f,
).apply {
    path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black),
        pathFillType = PathFillType.EvenOdd) {
        moveTo(28.342f, 15.834f)
        lineTo(10.968f, 32.648f)
        curveTo(8.847f, 31.397f, 7.043f, 29.666f, 5.706f, 27.604f)
        lineTo(18.261f, 15.834f)
        lineTo(28.342f, 15.834f)
        close()
        moveTo(26.559f, 5.084f)
        lineTo(4.596f, 25.583f)
        curveTo(3.678f, 23.579f, 3.167f, 21.349f, 3.167f, 19.000f)
        curveTo(3.167f, 10.256f, 10.256f, 3.167f, 19.000f, 3.167f)
        curveTo(21.738f, 3.167f, 24.313f, 3.862f, 26.559f, 5.084f)
        close()
        moveTo(13.064f, 33.683f)
        curveTo(14.897f, 34.425f, 16.901f, 34.834f, 19.000f, 34.834f)
        curveTo(26.090f, 34.834f, 32.091f, 30.174f, 34.109f, 23.750f)
        lineTo(23.328f, 23.750f)
        lineTo(13.064f, 33.683f)
        close()
    }
}.build()

/** The app mark: white "e" disc on the gradient-A tile (Ernest's pick:
 *  Google blue #0099E8 -> violet #7B3FE4 -> pink #D63FA0). This is the
 *  ONLY mark the app shows — the v1.0 signal-bars mark is gone. */
@Composable
fun EtkMark(modifier: Modifier = Modifier, big: Boolean = false) {
    Box(
        modifier.clip(RoundedCornerShape(if (big) 30.dp else 14.dp))
            .background(BrandGradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(EtkMarkVector, contentDescription = null,
            tint = Color.White, modifier = Modifier.fillMaxSize(0.72f))
    }
}

/** A monochrome variant for dark surfaces: disc in the brand violet. */
@Composable
fun EtkMarkMono(modifier: Modifier = Modifier) {
    Icon(EtkMarkVector, contentDescription = null,
        tint = Color(0xFF7B3FE4), modifier = modifier)
}
