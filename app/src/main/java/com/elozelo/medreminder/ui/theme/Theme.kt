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
    primaryContainer = MedicalLightBlue,
    onPrimaryContainer = Color.Black,

    secondary = MedicalTealDark,
    onSecondary = MedicalDarkBg,
    secondaryContainer = MedicalMintDark,
    onSecondaryContainer = MedicalDarkBg,

    tertiary = Color(0xFF1A73E8),
    onTertiary = MedicalDarkBg,

    background = MedicalDarkBg,
    onBackground = MedicalWhite,
    surface = MedicalDarkSurface,
    onSurface = MedicalWhite,

    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = MedicalLightBlue,

    outline = MedicalBlueDark,

    error = Color(0xFFCF6679),
    onError = MedicalDarkBg
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalBlue,
    onPrimary = Color.White,
    primaryContainer = MedicalLightBlue,
    onPrimaryContainer = Color.Black,

    secondary = MedicalTeal,
    onSecondary = Color.White,
    secondaryContainer = MedicalMint,
    onSecondaryContainer = MedicalTeal,

    tertiary = Color(0xFF1A73E8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F5E9),
    onTertiaryContainer = MedicalGreen,

    background = MedicalWhite,
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),

    surfaceVariant = MedicalLightBlue,
    onSurfaceVariant = MedicalBlue,

    outline = MedicalBlue,

    error = Color(0xFFB00020),
    onError = Color.White
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