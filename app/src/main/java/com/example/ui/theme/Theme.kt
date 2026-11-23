package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val TacticalDarkColorScheme = darkColorScheme(
    primary = BeaconAmber,
    onPrimary = ObsidianNight,
    primaryContainer = BeaconAmberContainer,
    onPrimaryContainer = BeaconAmberBright,
    secondary = RadarCyan,
    onSecondary = ObsidianNight,
    secondaryContainer = RadarCyanContainer,
    onSecondaryContainer = RadarCyanBright,
    tertiary = SafeEmerald,
    onTertiary = ObsidianNight,
    tertiaryContainer = SafeEmeraldContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = Color(0xFFFCA5A5),
    background = ObsidianNight,
    onBackground = TextPrimaryDark,
    surface = MidnightSlate,
    onSurface = TextPrimaryDark,
    surfaceVariant = TacticalSurface,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSlate
)

private val TacticalLightColorScheme = lightColorScheme(
    primary = DaylightAmber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = DaylightCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = SafeEmerald,
    onTertiary = Color.White,
    error = AlertCrimson,
    onError = Color.White,
    background = DaylightBg,
    onBackground = DaylightNavy,
    surface = DaylightSurface,
    onSurface = DaylightNavy,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

val BlockWatchShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun BlockWatchTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TacticalDarkColorScheme else TacticalLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BlockWatchTypography,
        shapes = BlockWatchShapes,
        content = content
    )
}
