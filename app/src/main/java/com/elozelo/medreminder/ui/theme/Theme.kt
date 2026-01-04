package com.elozelo.medreminder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MedicalBlueDark,
    onPrimary = MedicalDarkBg,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = MedicalBlueDark,

    secondary = MedicalTealDark,
    onSecondary = MedicalDarkBg,
    secondaryContainer = Color(0xFF1E3D3B),
    onSecondaryContainer = MedicalTealDark,

    tertiary = WarmOrangeDark,
    onTertiary = MedicalDarkBg,
    tertiaryContainer = Color(0xFF3D2E1E),
    onTertiaryContainer = WarmOrangeDark,

    background = MedicalDarkBg,
    onBackground = MedicalWhite,
    surface = MedicalDarkSurface,
    onSurface = MedicalWhite,

    surfaceVariant = MedicalDarkCard,
    onSurfaceVariant = Color(0xFFB0B0B0),

    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF303030),

    error = Color(0xFFCF6679),
    onError = MedicalDarkBg,
    errorContainer = Color(0xFF3D1E1E),
    onErrorContainer = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalBlue,
    onPrimary = Color.White,
    primaryContainer = MedicalLightBlue,
    onPrimaryContainer = Color(0xFF0D47A1),

    secondary = MedicalTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF00695C),

    tertiary = WarmOrange,
    onTertiary = Color.White,
    tertiaryContainer = WarmOrangeLight,
    onTertiaryContainer = Color(0xFFE65100),

    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),

    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF5F5F5F),

    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFEEEEEE),

    error = WarmRed,
    onError = Color.White,
    errorContainer = WarmRedLight,
    onErrorContainer = WarmRed
)

@Composable
fun MedReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}